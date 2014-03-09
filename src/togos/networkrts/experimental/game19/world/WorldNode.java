package togos.networkrts.experimental.game19.world;

import java.util.List;

import togos.networkrts.util.BitAddressRange;

// TODO: Rename to RSTNode (Regular Solid Tree Node)
// Also rename subclasses accordingly.
public interface WorldNode extends BlockStack, BitAddressRange
{
	public enum NodeType {
		QUADTREE,
		BLOCKSTACK
	};
	
	public static final WorldNode[] EMPTY_LIST = new WorldNode[0];
	
	public NodeType getNodeType();
	public long getNextAutoUpdateTime();
	public Block[] getBlocks();
	public WorldNode[] getSubNodes();
	
	public WorldNode update( int x, int y, int sizePower, long time, Message[] messages, List<Action> results );
}
