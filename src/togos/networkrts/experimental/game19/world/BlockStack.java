package togos.networkrts.experimental.game19.world;

public class BlockStack
{
	public static final BlockStack EMPTY = new BlockStack( new Block[0] );
	
	public final Block[] blocks;
	
	private BlockStack( Block[] blocks ) {
		this.blocks = blocks;
	}
	
	public static BlockStack create( Block block ) {
		return block.stack == null ? new BlockStack( new Block[]{block} ) : block.stack;
	}
	
	public static BlockStack create( Block[] blocks ) {
		if( blocks.length == 0 ) return EMPTY;
		if( blocks.length == 1 ) return create( blocks[0] );
		return new BlockStack( blocks );
	}
}
