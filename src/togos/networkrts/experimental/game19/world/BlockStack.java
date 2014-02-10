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
	
	transient WorldLeafNode leafNode;
	public synchronized WorldLeafNode toLeafNode() {
		if( leafNode == null ) leafNode = WorldLeafNode.create(this);
		return leafNode;
	}
	
	public BlockStack with( Block toBeAdded ) {
		if( blocks.length == 0 ) return toBeAdded.stack;
		
		Block[] newBlocks = new Block[blocks.length+1];
		for( int i=0; i<blocks.length; ++i ) {
			newBlocks[i] = blocks[i];
		}
		newBlocks[blocks.length] = toBeAdded; 
		
		return create(newBlocks);
	}
	
	public BlockStack without( Block toBeRemoved ) {
		int matches = 0;
		for( int i=0; i<blocks.length; ++i ) {
			if( blocks[i].equals(toBeRemoved) ) {
				++matches;
			}
		}
		
		if( matches == 0 ) return this;
		if( matches == blocks.length ) return EMPTY;
		
		Block[] newBlocks = new Block[blocks.length-matches];
		for( int i=0, j=0; i<blocks.length; ++i ) {
			if( !blocks[i].equals(toBeRemoved) ) {
				newBlocks[j++] = blocks[i];
			}
		}
		
		return create(newBlocks);
	}
}
