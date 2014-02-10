package togos.networkrts.experimental.game19.world;

import java.util.List;

public class WorldLeafNode extends BaseWorldNode
{
	public static final WorldLeafNode EMPTY = create( BlockStack.EMPTY );
	
	protected final BlockStack blockStack;
	
	private WorldLeafNode( BlockStack blockStack, long minId, long maxId, long nextAutoUpdateTime ) {
		super( minId, maxId, nextAutoUpdateTime );
		this.blockStack = blockStack;
	}
	
	public static WorldLeafNode create( BlockStack blockStack ) {
		assert blockStack != null;
		
		if( blockStack.leafNode != null ) return blockStack.leafNode;
		
		long aut = Long.MAX_VALUE;
		long minId = IDs.GENERIC_NODE_ID;
		long maxId = IDs.GENERIC_NODE_ID;
		for( Block b : blockStack.blocks ) {
			long baut = b.behavior.getNextAutoUpdateTime();
			if( baut < aut ) aut = baut;
			maxId |= b.behavior.getMaxId();
			minId &= b.behavior.getMinId();
		}
		return new WorldLeafNode( blockStack, minId, maxId, aut );
	}
	
	static WorldLeafNode create( Block[] blocks ) {
		return WorldLeafNode.create( BlockStack.create(blocks) );
	}
	
	@Override public boolean isLeaf() { return true; }
	@Override public BlockStack getBlockStack() { return blockStack; }
	@Override public WorldNode[] getSubNodes() { return WorldNode.EMPTY_LIST; }
	
	@Override protected WorldNode _update( int x, int y, int sizePower, long time, Message[] messages, List<Action> results ) {
		// TODO: handle 'create block' messages here
		
		//int resCount0 = results.size();
		Block[] newBlocks = new Block[blockStack.blocks.length];
		boolean anyBlocksUpdated = false;
		for( int i=0; i<blockStack.blocks.length; ++i ) {
			newBlocks[i] = blockStack.blocks[i].behavior.update( blockStack.blocks[i], x, y, sizePower, time, messages, results );
			if( newBlocks[i] != blockStack.blocks[i] ) anyBlocksUpdated = true;
		}
		return anyBlocksUpdated ? WorldLeafNode.create(newBlocks) : this;
	}
}
