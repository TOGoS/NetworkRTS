package togos.networkrts.s64;

class GridWorld64
{
	public static final GridWorld64 EMPTY = new GridWorld64( GridNode64.createRecursiveNode(new Block[0]), 0, 0, 0 );
	
	public final GridNode64 topNode;
	public final int widthPower, heightPower;
	public final int topNodeSizePower;
	
	public GridWorld64( GridNode64 topNode, int widthPower, int heightPower, int topNodeSizePower ) {
		this.topNode = topNode;
		this.widthPower = widthPower;
		this.heightPower = heightPower;
		this.topNodeSizePower = topNodeSizePower;
	}
	
	public GridWorld64 withBlock( int sizePower, int x, int y, Block block ) {
		return new GridWorld64( topNode.withBlock(sizePower, x, y, block ), widthPower, heightPower, topNodeSizePower );
	}
	
	public GridWorld64 withBlock( Shape s, double minDetailSize, Block b ) {
		return new GridWorld64( topNode.withBlock(Math.pow(2,topNodeSizePower), 0, 0, s, minDetailSize, b ), widthPower, heightPower, topNodeSizePower );
	}
}
