package togos.networkrts.experimental.s64;

class GridNode64
{
	protected static final GridNode64 createRecursiveNode( Block[][] fillWith ) {
		GridNode64[] subNodes = new GridNode64[64];
		GridNode64 n = new GridNode64( subNodes, fillWith );
		for( int i=0; i<64; ++i ) subNodes[i] = n;
		return n;
	}
	
	protected static final GridNode64 createRecursiveNode( Block[] fillWith ) {
		Block[][] stacks = new Block[64][];
		for( int i=0; i<64; ++i ) stacks[i] = fillWith;
		return createRecursiveNode( stacks );
	}
	
	public final GridNode64[] subNodes;
	public final Block[][] blockStacks;
	
	public GridNode64( GridNode64[] subNodes, Block[][] blockStacks ) {
		this.subNodes = subNodes;
		this.blockStacks = blockStacks;
	}
	
	/**
	 * Upper bits of x and y are ignored.
	 * @param nodeSizePower size of node, relative to brush, e.g. 3 is 8 times as long on each side
	 */
	public GridNode64 withBlock( int nodeSizePower, int x, int y, Block block ) {
		int sx = (x >> (nodeSizePower-3)) & 0x7;
		int sy = (y >> (nodeSizePower-3)) & 0x7;
		
		Block[][] newStacks;
		GridNode64[] newSubNodes;
		if( nodeSizePower <= 0 ) {
			return createRecursiveNode( block.getStack() );
		} else if( nodeSizePower == 3 ) {
			newStacks = new Block[64][];
			newSubNodes = new GridNode64[64];
			for( int i=0; i<64; ++i ) newSubNodes[i] = subNodes[i];
			for( int i=0; i<64; ++i ) newStacks[i] = blockStacks[i];
			newStacks[sx+sy*8] = block.getStack();
			newSubNodes[sx+sy*8] = block.getRecursiveNode();
		} else {
			newStacks = blockStacks;
			newSubNodes = new GridNode64[64];
			for( int i=0; i<64; ++i ) newSubNodes[i] = subNodes[i];
			newSubNodes[sx+sy*8] = subNodes[sx+sy*8].withBlock( nodeSizePower - 3, x, y, block);
		}
		
		return new GridNode64( newSubNodes, newStacks );
	}

	public GridNode64 withBlock(double nodeSize, double nodeX, double nodeY, Shape s, double minDetailSize, Block b) {
		int inclusiveness = s.includes(nodeX, nodeY, nodeSize, nodeSize);
		
		if( nodeSize <= minDetailSize ) {
			if( inclusiveness == Shape.INCLUDES_SOME ) inclusiveness = Shape.INCLUDES_ALL;
		}
		
		switch( inclusiveness ) {
		case( Shape.INCLUDES_NONE ): return this;
		case( Shape.INCLUDES_ALL ): return b.getRecursiveNode();
		}
		
		Block[][] newStacks = new Block[64][];
		GridNode64[] newSubNodes = new GridNode64[64];
		double subNodeSize = nodeSize / 8; 
		for( int sy=0, i=0; sy<8; ++sy ) {
			for( int sx=0; sx<8; ++sx, ++i ) {
				double snX = nodeX + subNodeSize*sx;
				double snY = nodeY + subNodeSize*sy;
				switch( s.includes(snX, snY, subNodeSize, subNodeSize) ) {
				case( Shape.INCLUDES_NONE ):
					newStacks[i] = blockStacks[i];
					newSubNodes[i] = subNodes[i];
					continue;
				case( Shape.INCLUDES_ALL ):
					newStacks[i] = b.getStack();
					newSubNodes[i] = b.getRecursiveNode();
					continue;
				case( Shape.INCLUDES_SOME ):
					newSubNodes[i] = subNodes[i].withBlock(subNodeSize, snX, snY, s, minDetailSize, b);
					newStacks[i] = newSubNodes[i].blockStacks[36];
					continue;
				}
			}
		}
		
		return new GridNode64( newSubNodes, newStacks );
	}
}
