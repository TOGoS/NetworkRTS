package togos.networkrts.experimental.game19.world;

public class BlockInstance extends NodePosition
{
	public final int indexInStack;
	public final Block block;
	
	public BlockInstance( int x, int y, int index, Block b ) {
		super(x, y, 0);
		this.indexInStack = index;
		this.block = b;
	}
}
