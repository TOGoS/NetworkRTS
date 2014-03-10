package togos.networkrts.experimental.game19.world;

import java.util.Collection;
import java.util.List;

import togos.networkrts.util.BitAddressRange;

/**
 * Regular Solid Tree Node.
 * Used to represent tile data.
 */
public interface RSTNode extends BlockStack, BitAddressRange
{
	public enum NodeType {
		QUADTREE,
		BLOCKSTACK
	};
	
	public static final RSTNode[] EMPTY_LIST = new RSTNode[0];
	
	public NodeType getNodeType();
	public long getNextAutoUpdateTime();
	public Block[] getBlocks();
	public RSTNode[] getSubNodes();
	
	public RSTNode update( int x, int y, int sizePower, long time, Collection<Message> messages, List<Action> results );
}
