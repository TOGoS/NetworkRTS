package togos.networkrts.experimental.forthbots;

import java.util.Arrays;

import togos.networkrts.util.Freezable;
import togos.networkrts.util.FrozenModificationException;
import togos.networkrts.util.Thawable;

public class ForthBotsWorld {
	public static class ForthVM implements Freezable<ForthVM>, Thawable<ForthVM> {
		/** When set, allows messages to be received at the memory location defined by MESSAGE_RECV_BUF_REG */
		public static final short MESSAGE_RECEIVE  = 0x1;
		/** When set, indicates that a message has been received */
		public static final short MESSAGE_RECEIVED = 0x2;
		/** When set, signals to the hardware that the message in the send buffer should be sent */
		public static final short MESSAGE_SEND     = 0x4;
		
		public static final short PC_REG         = 0;
		public static final short PC_RESET_REG   = 1;
		public static final short DS_START_REG   = 2;
		public static final short DS_END_REG     = 3;
		public static final short PS_START_REG   = 4;
		public static final short PS_END_REG     = 5;
		public static final short DS_REG         = 6;
		public static final short PS_REG         = 7;
		public static final short MOVEMENT_REG     = 16;
		public static final short WALL_SENSORS_REG = 17; // clockwise from east, northeast being 15
		public static final short MESSAGE_STATUS_REG = 17;
		public static final short MESSAGE_RECV_BUF_REG = 18;
		public static final short MESSAGE_RECV_SIZE_REG = 19;
		public static final short MESSAGE_SEND_BUF_REG = 20;
		public static final short MESSAGE_SEND_SIZE_REG = 21;
		public static final short DS_START_DEFAULT =  512;
		public static final short DS_END_DEFAULT   =  768;
		public static final short PS_START_DEFAULT =  768;
		public static final short PS_END_DEFAULT   = 1024;
		// Normal data stack area is 512-768
		// Normal program stack is 768-1024
		public static final short PC_RESET_VALUE = 1024;
		
		/**
		 * Instructions 0x0000-0x3FFF and 0xC000-0xFFFF push their own value onto the stack
		 * Other instructions are as follows.
		 */
		
		// Memory and stack ops
		public static final short I_FETCH = 0x4001; // (memory location -- value at that memory location)
		public static final short I_STORE = 0x4002; // (new value, memory location --)
		public static final short I_DUP   = 0x4003; // (a -- a, a)
		public static final short I_SWAP  = 0x4004; // (a, b -- b, a)
		public static final short I_DROP  = 0x4005; // (a --)
		public static final short I_PICK  = 0x4006; // (n -- thing from n down the stack (not counting n itself))
		
		// Arithmetic
		public static final short I_AND   = 0x4010; // (value 1, value 2 -- bitwise-ANDed value)
		public static final short I_OR    = 0x4011; // (value 1, value 2 -- bitwise-ORed value)
		public static final short I_XOR   = 0x4012; // (value 1, value 2 -- bitwise-XORed value)
		public static final short I_ADD   = 0x4013; // (value 1, value 2 -- value 1 + value 2)
		public static final short I_SUB   = 0x4014; // (value 1, value 2 -- value 1 - value 2)
		public static final short I_MUL   = 0x4015; // (value 1, value 2 -- value 1 * value 2)
		public static final short I_DIV   = 0x4016; // (value 1, value 2 -- value 1 / value 2)
		
		// Program flow
		public static final short I_CALL  = 0x4020; // Call function at location popped from top of stack
		public static final short I_JUMP  = 0x4021; // Jump the the location popped from top of stack
		public static final short I_WAIT  = 0x4022; // Wait for next simulation step
		public static final short I_JNZ   = 0x4023; // (value, jump offset --)
		public static final short I_JZ    = 0x4024; // (value, jump offset --)
		
		boolean frozen = false;
		final short[] memory;
		
		public ForthVM( short[] image ) {
			this.memory = Arrays.copyOf(image, image.length);
		}
		
		public short fetch(int index) {
			if( index < 0 || index >= memory.length ) return 0;
			return memory[index];
		}
		public short fetch(short index) {
			return fetch(index&0xFFFF);
		}
		public int fetchUint(short index) {
			return fetch(index)&0xFFFF;
		}
		
		// This should be the only function that modifies memory!
		// And it will throw a FrozenModificationException if the VM is frozen
		public void put(int index, short value) {
			if( frozen ) throw new FrozenModificationException();
			if( index < 0 || index >= memory.length ) return;
			memory[index] = value;
		}
		public void put(short index, short value) {
			put(index&0xFFFF, value);
		}
		
		public void push(short v) {
			// TODO: prevent overflow
			short ds = (short)(fetch(DS_REG) - 1);
			put(DS_REG, ds);
			put(ds, v);
		}
		public short peek(int n) {
			// TODO: prevent underflow
			short ds = fetch(DS_REG);
			return fetch(ds+n);
		}
		public short pop() {
			// TODO: prevent underflow
			short ds = fetch(DS_REG);
			short v = fetch(ds);
			put(DS_REG, (short)(ds+1));
			return v;
		}
		
		public void pushPs(short v) {
			// TODO: prevent overflow
			short ps = (short)(fetch(PS_REG) - 1);
			put(PS_REG, ps);
			put(ps, v);
		}
		public short popPs() {
			// TODO: prevent underflow
			short ps = fetch(PS_REG);
			short v = fetch(ps);
			put(PS_REG, (short)(ps+1));
			return v;
		}
		
		static class InvalidInstructionException extends Exception {
			private static final long serialVersionUID = 1L;
		}
		
		protected void doNormalInstruction(short inst) throws InvalidInstructionException {
			switch( inst ) {
			case I_FETCH:
				push(fetch(pop()));
				return;
			case I_STORE: {
				short l = pop();
				short v = pop();
				put(l, v);
				return;
			}
			case I_DUP:
				push(peek(0));
				return;
			case I_DROP:
				pop();
				return;
			case I_SWAP:
				short a = pop();
				short b = pop();
				push(a);
				push(b);
				return;
			case I_PICK:
				short n = pop();
				push(peek(n));
				return;
			case I_AND:
				push((short)(pop() & pop()));
				return;
			case I_OR:
				push((short)(pop() | pop()));
				return;
			case I_XOR:
				push((short)(pop() ^ pop()));
				return;
			case I_ADD:
				push((short)(pop() + pop()));
				return;
			case I_SUB: {
				short subtrahend = pop();
				short minuend = pop();
				push((short)(minuend - subtrahend));
				return;
			}
			case I_MUL:
				push((short)(pop() * pop()));
				return;
			case I_DIV: {
				short divisor = pop();
				short dividend = pop();
				push((short)(dividend / divisor));
				return;
			}
			default:
				if( (inst & 0xC000) == 0 || (inst & 0xC000) == 0xC000 ) {
					push(inst);
				} else {
					throw new InvalidInstructionException();
				}
			}
		}
		
		protected boolean doInstruction(short inst, short pc) {
			switch( inst ) {
			case I_WAIT:
				put(PC_REG, (short)(pc+1));
				return false;
			case I_JUMP:
				put(PC_REG, pop());
				return true;
			default:
				try {
					doNormalInstruction(inst);
					put(PC_REG, (short)(pc + 1));
					return true;
				} catch( InvalidInstructionException e ) {
					put(PC_REG, fetch(PC_RESET_REG));
					return false;
				}
			}
		}
		
		public boolean doInstruction(short inst) {
			short pc = fetch(PC_REG);
			return doInstruction(inst, pc);
		}
		
		public boolean step() {
			short pc = fetch(PC_REG);
			if( pc < 0 || pc >= memory.length ) pc = fetch(PC_RESET_REG);
			if( pc < 0 || pc >= memory.length ) pc = PC_RESET_VALUE;
			
			short inst = fetch(pc);
			
			if( pc >= memory.length ) return false;
			
			return doInstruction(inst);
		}
		
		public void step(int steps) {
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
		
		short moveFlags = tile.vm.fetch(ForthVM.MOVEMENT_REG);
		int moveX = x, moveY = y;
		if( (moveFlags & 0x0001) != 0 ) ++moveX;
		if( (moveFlags & 0x0002) != 0 ) ++moveY;
		if( (moveFlags & 0x0004) != 0 ) --moveX;
		if( (moveFlags & 0x0008) != 0 ) --moveY;
		tile.vm.put(ForthVM.MOVEMENT_REG, (short)0);
		
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
