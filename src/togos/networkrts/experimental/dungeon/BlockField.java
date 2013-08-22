package togos.networkrts.experimental.dungeon;

class BlockField
{
	final int w, h, d;
	Block[][] blockStacks;
	int[] light;
	
	public BlockField( int w, int h, int d, Block[] fill ) {
		this.w = w; this.h = h; this.d = d;
		this.blockStacks = new Block[w*h*d][];
		this.light = new int[w*h*d];
		fill( fill );
	}
	
	public void fill( Block[] stack ) {
		for( int i=0; i<blockStacks.length; ++i ) {
			blockStacks[i] = stack;
		}
	}
	
	public void clear() { fill(Block.EMPTY_STACK); }
	
	protected final int stackIndex( int x, int y, int z ) {
		return w*h*NumUtil.tmod(z,d)+w*NumUtil.tmod(y,h)+NumUtil.tmod(x,w);
	}
	
	public void setStack( int x, int y, int z, Block[] stack ) {
		if( x < 0 || x >= w || y < 0 || y >= h ) return;
		blockStacks[stackIndex(x,y,z)] = stack;
	}
	
	public Block[] getStack( int x, int y, int z ) {
		if( x < 0 || x >= w || y < 0 || y >= h ) return null;
		return blockStacks[stackIndex(x,y,z)];
	}
	
	public void addBlock( int x, int y, int z, Block b ) {
		if( x < 0 || x >= w || y < 0 || y >= h ) return;
		int index = stackIndex(x,y,z);
		Block[] stack = blockStacks[index];
		if( stack.length == 0 ) {
			// Simply replace the stack pointer! 
			blockStacks[index] = b.stack;
			return;
		}
		// Otherwise we need to build a new stack :(
		for( int j=0; j<stack.length; ++j ) {
			if( stack[j] == b ) return;
		}
		Block[] newStack = new Block[stack.length+1];
		for( int j=0; j<stack.length; ++j ) newStack[j] = stack[j];
		newStack[stack.length] = b;
		blockStacks[index] = newStack;
	}
	
	public void removeBlock( int x, int y, int z, Block b ) {
		if( x < 0 || x >= w || y < 0 || y >= h ) return;
		int index = stackIndex(x,y,z);
		Block[] stack = blockStacks[index];
		if( stack == null ) return;
		if( stack.length == 0 ) return;
		for( int j=0; j<stack.length; ++j ) {
			if( stack[j] == b ) {
				Block[] newStack;
				if( stack.length == 2 ) {
					newStack = stack[j^1].stack;
				} else {
					newStack = new Block[stack.length-1];
					for( int k=0; k<j; ++k ) newStack[j] = stack[k];
					for( int k=j+1; k<stack.length; ++k ) newStack[k-1] = stack[k];
				}
				blockStacks[index] = newStack;
			}
		}
	}
}
