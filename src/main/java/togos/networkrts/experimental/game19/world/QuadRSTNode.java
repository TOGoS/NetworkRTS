package togos.networkrts.experimental.game19.world;

import java.io.IOException;
import java.io.OutputStream;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.InvalidEncoding;
import togos.networkrts.experimental.game19.io.CerealWorldIO;
import togos.networkrts.experimental.game19.io.WorldObjectCCCodec;
import togos.networkrts.experimental.game19.sim.UpdateContext;
import togos.networkrts.util.HasURI;
import togos.networkrts.util.ResourceNotFound;

public class QuadRSTNode extends BaseRSTNode
{
	public static final WorldObjectCCCodec<QuadRSTNode> CCC = new WorldObjectCCCodec<QuadRSTNode>() {
		@Override public Class<QuadRSTNode> getEncodableClass() { return QuadRSTNode.class; }
		
		@Override public void encode(
			QuadRSTNode node, byte[] constructorPrefix, OutputStream os, CerealWorldIO cwio
		) throws IOException {
			RSTNode[] subNodes = node.getSubNodes();
			cwio.writeObjectReference(subNodes[0], os);
			cwio.writeObjectReference(subNodes[1], os);
			cwio.writeObjectReference(subNodes[2], os);
			cwio.writeObjectReference(subNodes[3], os);
			os.write(constructorPrefix);
		}
		
		@Override public int decode(
			byte[] data, int offset, DecodeState ds, CerealDecoder context
		) throws InvalidEncoding, ResourceNotFound {
			// Can't load lazily because we lack metadata
			// which is dependent on all subnodes, which this codec
			// doesn't store.
			RSTNode[] subNodes = new RSTNode[4];
			for( int i=3; i>=0; --i ) {
				Object obj = ds.removeStackItem(-1);
				if( obj instanceof HasURI ) {
					obj = context.get(((HasURI)obj).getUri());
				}
				if( obj instanceof Block ) {
					obj = ((Block)obj).stack;
				}
				if( obj instanceof RSTNode ) {
					subNodes[i] = (RSTNode)obj;
				} else {
					throw new InvalidEncoding("Expected an RSTNode or Block, found a "+obj.getClass());
				}
			}
			ds.pushStackItem( QuadRSTNode.create(subNodes) );
			return offset;
		}
	};
	
	protected final RSTNode[] subNodes;
	
	private QuadRSTNode( RSTNode[] subNodes, long minId, long maxId, long nextAutoUpdateTime ) {
		super( minId, maxId, nextAutoUpdateTime );
		assert subNodes != null;
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
		MessageSet messages, UpdateContext updateContext
	) {
		RSTNode[] newSubNodes = new RSTNode[4];
		int subSizePower = sizePower-1;
		int subSize = 1<<subSizePower;
		for( int sy=0, si=0; sy<2; ++sy) for( int sx=0; sx<2; ++sx, ++si ) {
			newSubNodes[si] = subNodes[si].update( x+(sx*subSize), y+(sy*subSize), subSizePower, time, messages, updateContext );
		}
		return QuadRSTNode.create( newSubNodes ); 
	}
}
