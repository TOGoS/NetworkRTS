package togos.networkrts.experimental.game19.world;

import java.util.List;

public interface WorldNode
{
	public static final WorldNode[] EMPTY_LIST = new WorldNode[0];
	
	public boolean isLeaf();
	public long getMinId();
	public long getMaxId();
	public long getNextAutoUpdateTime();
	public BlockStack getBlockStack();
	public WorldNode[] getSubNodes();
	
	public WorldNode update( int x, int y, int sizePower, long time, Message[] messages, List<Action> results );
}
