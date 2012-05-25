package togos.networkrts.experimental.s64;

import togos.networkrts.experimental.s64.fill.GridNode64Filler;

public class GridNode64
{
	public final GridNode64[] subNodes;
	public final Block[][] blockStacks;
	/** Used for merging functionally similar nodes */
	public final Object canonicalId;
	
	/**
	 * If you use this constructor, you should immediately fill
	 * in subNode and blockStack elements!
	 */
	protected GridNode64() {
		this.subNodes = new GridNode64[64];
		this.blockStacks = new Block[64][];
		this.canonicalId = this;
	}
	
	protected GridNode64( Object canonicalId ) {
		this.subNodes = new GridNode64[64];
		this.blockStacks = new Block[64][];
		this.canonicalId = canonicalId;
	}

	public GridNode64( GridNode64[] subNodes, Block[][] blockStacks ) {
		assert subNodes.length == 64;
		assert blockStacks.length == 64;
		this.subNodes = subNodes;
		this.blockStacks = blockStacks;
		this.canonicalId = this;
	}
	
	public boolean isHomogeneous() {
		return false;
	}
	
	protected static boolean identicalAndHomogeneous( GridNode64[] nodes ) {
		assert nodes.length > 0;
		
		if( !nodes[0].isHomogeneous() ) return false;
		for( int i=0; i<nodes.length; ++i ) {
			if( nodes[i].canonicalId != nodes[0].canonicalId ) return false;
		}
		return true;
	}

	public GridNode64 fillArea(double nodeSize, double nodeX, double nodeY, Shape s, double minDetailSize, GridNode64Filler fill) {
		int inclusiveness = s.includes(nodeX, nodeY, nodeSize, nodeSize);
		
		if( nodeSize <= minDetailSize ) {
			if( inclusiveness == Shape.INCLUDES_SOME ) inclusiveness = Shape.INCLUDES_ALL;
		}
		
		switch( inclusiveness ) {
		case( Shape.INCLUDES_NONE ): return this;
		case( Shape.INCLUDES_ALL ): return fill.getNode( nodeX, nodeY, nodeSize );
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
					GridNode64 n = fill.getNode( snX, snY, subNodeSize );
					newSubNodes[i] = n;
					newStacks[i] = n.blockStacks[36];
					continue;
				case( Shape.INCLUDES_SOME ):
					newSubNodes[i] = subNodes[i].fillArea(subNodeSize, snX, snY, s, minDetailSize, fill);
					newStacks[i] = newSubNodes[i].blockStacks[36];
					continue;
				}
			}
		}
		
		if( identicalAndHomogeneous(newSubNodes) ) {
			return newSubNodes[0];
		}
		
		return new GridNode64( newSubNodes, newStacks );
	}
}
