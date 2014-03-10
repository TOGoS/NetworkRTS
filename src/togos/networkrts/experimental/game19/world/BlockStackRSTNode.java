package togos.networkrts.experimental.game19.world;

import java.util.Collection;
import java.util.List;

public class BlockStackRSTNode extends BaseRSTNode
{
	public static final BlockStackRSTNode EMPTY = BlockStackRSTNode.create( new Block[0] );
	
	protected final Block[] blocks;
	
	private BlockStackRSTNode( Block[] blocks, long minId, long maxId, long nextAutoUpdateTime ) {
		super( minId, maxId, nextAutoUpdateTime );
		this.blocks = blocks;
	}
	
	static BlockStackRSTNode create( Block[] blocks ) {
		if( blocks.length == 0 && EMPTY != null ) return EMPTY;
		
		long aut = Long.MAX_VALUE;
		long minAddress = BitAddresses.TYPE_NODE;
		long maxAddress = BitAddresses.TYPE_NODE;
		for( Block b : blocks ) {
			long baut = b.getNextAutoUpdateTime();
			if( baut < aut ) aut = baut;
			maxAddress |= b.getMaxBitAddress();
			minAddress &= b.getMinBitAddress();
		}
		return new BlockStackRSTNode( blocks, minAddress, maxAddress, aut );
	}
	
	public static BlockStackRSTNode create( Block block ) {
		if( block.stack != null ) return block.stack;
		return create( new Block[]{ block } );
	}
	
	@Override public NodeType getNodeType() { return NodeType.BLOCKSTACK; }
	@Override public Block[] getBlocks() { return blocks; }
	@Override public RSTNode[] getSubNodes() { return RSTNode.EMPTY_LIST; }
	
	@Override protected RSTNode _update( int x, int y, int sizePower, long time, Collection<Message> messages, List<Action> results ) {
		// TODO: handle 'create block' messages here
		
		//int resCount0 = results.size();
		Block[] newBlocks = new Block[blocks.length];
		boolean anyBlocksUpdated = false;
		for( int i=0; i<blocks.length; ++i ) {
			newBlocks[i] = blocks[i].behavior.update( blocks[i], x, y, sizePower, time, messages, results );
			if( newBlocks[i] != blocks[i] ) anyBlocksUpdated = true;
		}
		return anyBlocksUpdated ? BlockStackRSTNode.create(newBlocks) : this;
	}

	public static RSTNode withoutBlock(RSTNode n, Block toBeRemoved) {
		int matches = 0;
		Block[] blocks = n.getBlocks();
		
		for( int i=0; i<blocks.length; ++i ) if( blocks[i].equals(toBeRemoved) ) ++matches;
		
		if( matches == 0 ) return n;
		if( matches == blocks.length ) return EMPTY;
		
		Block[] newBlocks = new Block[blocks.length-matches];
		for( int i=0, j=0; i<blocks.length; ++i ) {
			if( !blocks[i].equals(toBeRemoved) ) {
				newBlocks[j++] = blocks[i];
			}
		}
		
		return create(newBlocks);
	}
	
	public static RSTNode withBlock(RSTNode n, Block toBeAdded) {
		Block[] blocks = n.getBlocks();
		if( blocks.length == 0 ) return toBeAdded.stack;
		
		Block[] newBlocks = new Block[blocks.length+1];
		for( int i=0; i<blocks.length; ++i ) {
			newBlocks[i] = blocks[i];
		}
		newBlocks[blocks.length] = toBeAdded; 
		
		return create(newBlocks);
	}
}
