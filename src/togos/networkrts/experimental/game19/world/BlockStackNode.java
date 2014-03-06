package togos.networkrts.experimental.game19.world;

import java.util.List;

public class BlockStackNode extends BaseWorldNode
{
	public static final BlockStackNode EMPTY = BlockStackNode.create( new Block[0] );
	
	protected final Block[] blocks;
	
	private BlockStackNode( Block[] blocks, long minId, long maxId, long nextAutoUpdateTime ) {
		super( minId, maxId, nextAutoUpdateTime );
		this.blocks = blocks;
	}
	
	static BlockStackNode create( Block[] blocks ) {
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
		return new BlockStackNode( blocks, minAddress, maxAddress, aut );
	}
	
	public static BlockStackNode create( Block block ) {
		if( block.stack != null ) return block.stack;
		return create( new Block[]{ block } );
	}
	
	@Override public NodeType getNodeType() { return NodeType.BLOCKSTACK; }
	@Override public Block[] getBlocks() { return blocks; }
	@Override public WorldNode[] getSubNodes() { return WorldNode.EMPTY_LIST; }
	
	@Override protected WorldNode _update( int x, int y, int sizePower, long time, Message[] messages, List<Action> results ) {
		// TODO: handle 'create block' messages here
		
		//int resCount0 = results.size();
		Block[] newBlocks = new Block[blocks.length];
		boolean anyBlocksUpdated = false;
		for( int i=0; i<blocks.length; ++i ) {
			newBlocks[i] = blocks[i].behavior.update( blocks[i], x, y, sizePower, time, messages, results );
			if( newBlocks[i] != blocks[i] ) anyBlocksUpdated = true;
		}
		return anyBlocksUpdated ? BlockStackNode.create(newBlocks) : this;
	}

	public static WorldNode withoutBlock(WorldNode n, Block toBeRemoved) {
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
	
	public static WorldNode withBlock(WorldNode n, Block toBeAdded) {
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
