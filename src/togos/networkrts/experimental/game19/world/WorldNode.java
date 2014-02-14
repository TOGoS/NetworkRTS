package togos.networkrts.experimental.game19.world;

import java.util.List;

import togos.networkrts.util.BitAddressRange;

public interface WorldNode extends BitAddressRange
{
	public static final WorldNode[] EMPTY_LIST = new WorldNode[0];
	
	public boolean isLeaf();
	public long getNextAutoUpdateTime();
	public BlockStack getBlockStack();
	public WorldNode[] getSubNodes();
	
	public WorldNode update( int x, int y, int sizePower, long time, Message[] messages, List<Action> results );
}
