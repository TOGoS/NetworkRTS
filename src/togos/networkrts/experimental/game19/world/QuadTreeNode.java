package togos.networkrts.experimental.game19.world;

import java.util.List;

public class QuadTreeNode extends BaseWorldNode
{
	protected final WorldNode[] subNodes;
	
	private QuadTreeNode( WorldNode[] subNodes, long minId, long maxId, long nextAutoUpdateTime ) {
		super( minId, maxId, nextAutoUpdateTime );
		assert subNodes.length == 4;
		this.subNodes = subNodes;
	}
	
	public static QuadTreeNode create( WorldNode[] subNodes ) {
		long aut = Long.MAX_VALUE;
		long minId = BitAddresses.TYPE_NODE;
		long maxId = BitAddresses.TYPE_NODE;
		for( WorldNode n : subNodes ) {
			long baut = n.getNextAutoUpdateTime();
			if( baut < aut ) aut = baut;
			maxId |= n.getMaxBitAddress();
			minId &= n.getMinBitAddress();
		}
		return new QuadTreeNode( subNodes, minId, maxId, aut );
	}
	
	/**
	 * Creates a new node unless all subnodes would be identical
	 * to the corresponding ones in oldNode, in which case the old node
	 * will be returned
	 */
	public static WorldNode createBasedOn( WorldNode[] newSubNodes, WorldNode oldNode ) {
		WorldNode[] oldSubNodes = oldNode.getSubNodes();
		for( int i=0; i<4; ++i ) {
			if( newSubNodes[i] != oldSubNodes[i] ) {
				return create(newSubNodes);
			}
		}
		return oldNode;
	}
	
	public static WorldNode createHomogeneousQuad( WorldNode subNode ) {
		return create( new WorldNode[] { subNode, subNode, subNode, subNode } );
	}
	
	@Override public NodeType getNodeType() { return NodeType.QUADTREE; }
	@Override public BlockStack getBlockStack() { return BlockStack.EMPTY; }
	@Override public WorldNode[] getSubNodes() { return subNodes; }
	
	@Override protected WorldNode _update(
		int x, int y, int sizePower, long time,
		Message[] messages, List<Action> results
	) {
		WorldNode[] newSubNodes = new WorldNode[4];
		int subSizePower = sizePower-1;
		int subSize = 1<<subSizePower;
		for( int sy=0, si=0; sy<2; ++sy) for( int sx=0; sx<2; ++sx, ++si ) {
			newSubNodes[si] = subNodes[si].update( x+(sx*subSize), y+(sy*subSize), subSizePower, time, messages, results );
		}
		return QuadTreeNode.create( newSubNodes ); 
	}
}
