package togos.networkrts.experimental.s64;

import togos.networkrts.experimental.s64.fill.GridNode64Filler;

class GridWorld64
{
	public static final GridWorld64 EMPTY = new GridWorld64( new HomogeneousGridNode64(Block.EMPTY_STACK), 1, 1, 1 );
	
	public final GridNode64 topNode;
	public final double width, height;
	public final double topNodeSize;
	
	public GridWorld64( GridNode64 topNode, double width, double height, double topNodeSize ) {
		this.topNode = topNode;
		this.width = width;
		this.height = height;
		this.topNodeSize = topNodeSize;
	}
	
	public GridWorld64 fillArea( Shape s, double minDetailSize, GridNode64Filler n ) {
		return new GridWorld64( topNode.fillArea(topNodeSize, 0, 0, s, minDetailSize, n ), width, height, topNodeSize );
	}
}
