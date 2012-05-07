package togos.networkrts.experimental.s64;

class GridNode64
{
	public final GridNode64[] subNodes;
	public final Block[][] blockStacks;
	
	/**
	 * If you use this constructor, you should immediately fill
	 * in subNode and blockStack elements!
	 */
	protected GridNode64() {
		subNodes = new GridNode64[64];
		blockStacks = new Block[64][];
	}
	
	public GridNode64( GridNode64[] subNodes, Block[][] blockStacks ) {
		this.subNodes = subNodes;
		this.blockStacks = blockStacks;
	}
	
	public boolean isHomogeneous() {
		return false;
	}

	public GridNode64 fillArea(double nodeSize, double nodeX, double nodeY, Shape s, double minDetailSize, GridNode64 n) {
		int inclusiveness = s.includes(nodeX, nodeY, nodeSize, nodeSize);
		
		if( nodeSize <= minDetailSize ) {
			if( inclusiveness == Shape.INCLUDES_SOME ) inclusiveness = Shape.INCLUDES_ALL;
		}
		
		switch( inclusiveness ) {
		case( Shape.INCLUDES_NONE ): return this;
		case( Shape.INCLUDES_ALL ): return n;
		}
		
		Block[][] newStacks = new Block[64][];
		GridNode64[] newSubNodes = new GridNode64[64];
		double subNodeSize = nodeSize / 8; 
		for( int i=0, sy=0; sy<8; ++sy ) {
			for( int sx=0; sx<8; ++sx, ++i ) {
				double snX = nodeX + subNodeSize*sx;
				double snY = nodeY + subNodeSize*sy;
				switch( s.includes(snX, snY, subNodeSize, subNodeSize) ) {
				case( Shape.INCLUDES_NONE ):
					newSubNodes[i] = subNodes[i];
					newStacks[i] = blockStacks[i];
					continue;
				case( Shape.INCLUDES_ALL ):
					newSubNodes[i] = n;
					newStacks[i] = n.blockStacks[36];
					continue;
				case( Shape.INCLUDES_SOME ):
					newSubNodes[i] = subNodes[i].fillArea(subNodeSize, snX, snY, s, minDetailSize, n);
					newStacks[i] = newSubNodes[i].blockStacks[36];
					continue;
				}
			}
		}
		
		return new GridNode64( newSubNodes, newStacks );
	}
}
