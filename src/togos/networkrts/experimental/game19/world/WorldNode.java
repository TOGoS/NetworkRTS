package togos.networkrts.experimental.game19.world;

import java.util.List;

public interface WorldNode
{
	/** All nodes have an ID of 1 so they can receive messages */
	public static final long GENERIC_NODE_ID = 1;
	public static final WorldNode[] EMPTY_LIST = new WorldNode[0];
	
	public boolean isLeaf();
	public long getMinId();
	public long getMaxId();
	public long getNextAutoUpdateTime();
	public BlockStack getBlockStack();
	public WorldNode[] getSubNodes();
	
	public WorldNode update( int x, int y, int size, long time, Message[] messages, List<Action> results );
}
