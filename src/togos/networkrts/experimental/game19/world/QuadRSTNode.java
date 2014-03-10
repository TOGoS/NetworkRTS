package togos.networkrts.experimental.game19.world;

import java.util.Collection;
import java.util.List;

public class QuadRSTNode extends BaseRSTNode
{
	protected final RSTNode[] subNodes;
	
	private QuadRSTNode( RSTNode[] subNodes, long minId, long maxId, long nextAutoUpdateTime ) {
		super( minId, maxId, nextAutoUpdateTime );
		assert subNodes.length == 4;
		this.subNodes = subNodes;
	}
	
	public static QuadRSTNode create( RSTNode[] subNodes ) {
		long aut = Long.MAX_VALUE;
		long minId = BitAddresses.TYPE_NODE;
		long maxId = BitAddresses.TYPE_NODE;
		for( RSTNode n : subNodes ) {
			long baut = n.getNextAutoUpdateTime();
			if( baut < aut ) aut = baut;
			maxId |= n.getMaxBitAddress();
			minId &= n.getMinBitAddress();
		}
		return new QuadRSTNode( subNodes, minId, maxId, aut );
	}
	
	/**
	 * Creates a new node unless all subnodes would be identical
	 * to the corresponding ones in oldNode, in which case the old node
	 * will be returned
	 */
	public static RSTNode createBasedOn( RSTNode[] newSubNodes, RSTNode oldNode ) {
		RSTNode[] oldSubNodes = oldNode.getSubNodes();
		for( int i=0; i<4; ++i ) {
			if( newSubNodes[i] != oldSubNodes[i] ) {
				return create(newSubNodes);
			}
		}
		return oldNode;
	}
	
	public static RSTNode createHomogeneousQuad( RSTNode subNode ) {
		return create( new RSTNode[] { subNode, subNode, subNode, subNode } );
	}
	
	public static RSTNode createHomogeneous( RSTNode leaf, int depth ) {
		assert depth >= 0;
		
		return depth == 0 ? leaf : createHomogeneousQuad( createHomogeneous( leaf, depth-1 ) );
	}
	
	@Override public NodeType getNodeType() { return NodeType.QUADTREE; }
	@Override public Block[] getBlocks() { return BlockStackRSTNode.EMPTY.getBlocks(); }
	@Override public RSTNode[] getSubNodes() { return subNodes; }
	
	@Override protected RSTNode _update(
		int x, int y, int sizePower, long time,
		Collection<Message> messages, List<Action> results
	) {
		RSTNode[] newSubNodes = new RSTNode[4];
		int subSizePower = sizePower-1;
		int subSize = 1<<subSizePower;
		for( int sy=0, si=0; sy<2; ++sy) for( int sx=0; sx<2; ++sx, ++si ) {
			newSubNodes[si] = subNodes[si].update( x+(sx*subSize), y+(sy*subSize), subSizePower, time, messages, results );
		}
		return QuadRSTNode.create( newSubNodes ); 
	}
}
