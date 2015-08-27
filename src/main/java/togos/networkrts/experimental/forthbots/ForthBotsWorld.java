package togos.networkrts.experimental.forthbots;

import java.util.Arrays;

import togos.networkrts.util.Freezable;
import togos.networkrts.util.FrozenModificationException;
import togos.networkrts.util.Thawable;

public class ForthBotsWorld {
	static class ForthVM implements Freezable<ForthVM>, Thawable<ForthVM> {
		/** When set, allows messages to be received at the memory location defined by MESSAGE_RECV_BUF_REG */
		public static final short MESSAGE_RECEIVE  = 0x1;
		/** When set, indicates that a message has been received */
		public static final short MESSAGE_RECEIVED = 0x2;
		/** When set, signals to the hardware that the message in the send buffer should be sent */
		public static final short MESSAGE_SEND     = 0x4;
		
		public static final short PC_REG   = 0;
		public static final short MODE_REG = 1;
		public static final short DS_START_REG = 2;
		public static final short DS_END_REG   = 3;
		public static final short PS_START_REG = 4;
		public static final short PS_END_REG   = 5;
		public static final short DS_REG       = 6;
		public static final short PS_REG       = 7;
		public static final short WALL_SENSORS_REG = 8; // clockwise from east, northeast being 15
		public static final short MOVEMENT_REG = 16;
		public static final short MESSAGE_STATUS_REG = 17;
		public static final short MESSAGE_RECV_BUF_REG = 18;
		public static final short MESSAGE_RECV_SIZE_REG = 19;
		public static final short MESSAGE_SEND_BUF_REG = 20;
		public static final short MESSAGE_SEND_SIZE_REG = 21;
		// Normal data stack area is 512-768
		// Normal program stack is 768-1024
		public static final short PC_RESET_VALUE = 1024;
		
		/**
		 * Instructions 0x0000-0x3FFF and 0xC000-0xFFFF push their own value onto the stack
		 * Other instructions are as follows.
		 */
		public static final short I_FETCH = 0x4001; // (memory location -- value at that memory location)
		public static final short I_PUT   = 0x4002; // (new value, memory location --)
		public static final short I_AND   = 0x4005; // (value 1, value 2 -- bitwise-ANDed value)
		public static final short I_OR    = 0x4006; // (value 1, value 2 -- bitwise-ORed value)
		public static final short I_XOR   = 0x4007; // (value 1, value 2 -- bitwise-XORed value)
		
		public static final short I_CALL  = 0x4010; // Call function at location popped from top of stack
		public static final short I_JUMP  = 0x4011; // Jump the the location popped from top of stack
		public static final short I_WAIT  = 0x4012; // Wait for next simulation step
		public static final short I_JNZ   = 0x4013; // (value, jump offset --)
		public static final short I_JZ    = 0x4014; // (value, jump offset --)
		
		boolean frozen = false;
		final short[] memory;
		
		public ForthVM( short[] image ) {
			this.memory = Arrays.copyOf(image, image.length);
		}
		
		public short mem(short index) {
			if( index < 0 || index >= memory.length ) return 0;
			return memory[index];
		}
		
		public void mem(short index, short value) {
			if( index < 0 || index >= memory.length ) return;
			memory[index] = value;
		}
		
		protected void push(short v) {
			// TODO: prevent overflow
			short ds = (short)(mem(DS_REG) - 1);
			mem(DS_REG, ds);
			mem(ds, v);
		}
		
		protected short pop() {
			// TODO: prevent underflow
			short ds = mem(DS_REG);
			short v = mem(ds);
			mem(DS_REG, (short)(ds+1));
			return v;
		}
		
		public boolean step() {
			if( frozen ) throw new FrozenModificationException();
			short pc = mem(PC_REG);
			if( pc < 0 || pc >= memory.length ) pc = PC_RESET_VALUE;
			
			short inst = mem(pc);
			
			if( pc >= memory.length ) return false;
			
			switch( inst ) {
			case I_WAIT:
				mem(PC_REG, (short)(pc+1));
				return false;
			case I_FETCH:
				push(mem(pop()));
				mem(PC_REG, (short)(pc+1));
				return true;
			case I_PUT: {
				short l = pop();
				short v = pop();
				mem(l, v);
				mem(PC_REG, (short)(pc+1));
				return true;
			}
			case I_JUMP:
				mem(PC_REG, pop());
				return true;
			default:
				if( (inst & 0xC000) == 0 || (inst & 0xC000) == 0xC000 ) {
					push(inst);
					mem(PC_REG, (short)(pc+1));
				}
				return true;
			}
		}
		
		public void step(int steps) {
			if( frozen ) throw new FrozenModificationException();
			while( steps > 0 && step() ) --steps;
		}
		
		@Override public ForthVM freeze() {
			frozen = true;
			return this;
		}
		@Override public ForthVM thaw() {
			if( !frozen ) return this;
			return new ForthVM(this.memory);
		}
	}
	
	static class Tile implements Freezable<Tile>, Thawable<Tile> {
		public static final short FLAG_SOLID = 0x0001;
		public static final Tile EMPTY = new Tile();
		
		// Might be good to use a freeze/thaw system here...
		
		protected boolean frozen = false;
		public int iconNumber;
		public ForthVM vm;
		public short flags;
		public long nextAutoUpdateTime;
		
		public Tile freeze() {
			this.frozen = true;
			if(this.vm != null) this.vm = this.vm.freeze();
			return this;
		}
		public Tile thaw() {
			if( !this.frozen ) return this;
			Tile newTile = new Tile();
			newTile.iconNumber = iconNumber;
			newTile.vm = vm.thaw();
			newTile.flags = flags;
			newTile.nextAutoUpdateTime = nextAutoUpdateTime;
			return newTile;
		}
	}
	
	final int width, height;
	final Tile[] tiles;
	long stepNumber = 0;
	
	static int mod(int x, int by) {
		// TODO: A better one, someday
		while( x < 0 ) x += by;
		while( x >= by ) x -= by;
		return x;
	}
	
	int cellIdx(int x, int y) {
		return mod(x,width) + mod(y,height)*width;
	}
	
	public ForthBotsWorld(int width, int height) {
		this.width = width;
		this.height = height;
		this.tiles = new Tile[width*height];
		for( int i=tiles.length-1; i>=0; --i ) tiles[i] = Tile.EMPTY;
	}
	
	protected void update(int x, int y, Tile tile) {
		if( tile.nextAutoUpdateTime > stepNumber ) return;
		if( tile.vm == null ) return;
		
		final int srcIdx = cellIdx(x,y);
		tile = tiles[srcIdx] = tile.thaw();
		
		short moveFlags = tile.vm.mem(ForthVM.MOVEMENT_REG);
		int moveX = x, moveY = y;
		if( (moveFlags & 0x0001) != 0 ) ++moveX;
		if( (moveFlags & 0x0002) != 0 ) ++moveY;
		if( (moveFlags & 0x0004) != 0 ) --moveX;
		if( (moveFlags & 0x0008) != 0 ) --moveY;
		tile.vm.mem(ForthVM.MOVEMENT_REG, (short)0);
		
		if( moveX != x || moveY != y ) {
			final int destIdx = cellIdx(moveX,moveY);
			Tile destTile = tiles[destIdx];
			if( (destTile.flags & Tile.FLAG_SOLID) == 0 ) {
				// We can swap them!
				tiles[srcIdx] = destTile;
				tiles[destIdx] = tile;
			}
		}
		
		tile.nextAutoUpdateTime = stepNumber+1;
		tile.vm.step(16);
	}
	
	public void step() {
		for( int idx=0, row=0; row<height; ++row ) {
			for( int col=0; col<width; ++col, ++idx ) {
				update(col, row, tiles[idx]);
			}
		}
		++stepNumber;
	}
}
