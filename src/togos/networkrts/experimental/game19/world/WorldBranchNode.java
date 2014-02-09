package togos.networkrts.experimental.game19.world;

import java.util.List;

public class WorldBranchNode extends BaseWorldNode
{
	protected final WorldNode[] subNodes;
	
	private WorldBranchNode( WorldNode[] subNodes, long minId, long maxId, long nextAutoUpdateTime ) {
		super( minId, maxId, nextAutoUpdateTime );
		this.subNodes = subNodes;
	}
	
	public static WorldBranchNode create( WorldNode[] subNodes ) {
		long aut = Long.MAX_VALUE;
		long minId = WorldNode.GENERIC_NODE_ID;
		long maxId = WorldNode.GENERIC_NODE_ID;
		for( WorldNode n : subNodes ) {
			long baut = n.getNextAutoUpdateTime();
			if( baut < aut ) aut = baut;
			maxId |= n.getMaxId();
			minId &= n.getMinId();
		}
		return new WorldBranchNode( subNodes, minId, maxId, aut );
	}
	
	@Override public boolean isLeaf() { return false; }
	@Override public BlockStack getBlockStack() { return BlockStack.EMPTY; }
	@Override public WorldNode[] getSubNodes() { return subNodes; }
	@Override protected WorldNode _update(
		int x, int y, int size, long time,
		Message[] messages, List<Action> results
	) {
		WorldNode[] newSubNodes = new WorldNode[4];
		int subSize = size>>1;
		for( int sy=0, si=0; sy<2; ++sy) for( int sx=0; sy<2; ++sx, ++si ) {
			newSubNodes[si] = subNodes[si].update( x+(sx*subSize), y+(sy*subSize), subSize, time, messages, results );
		}
		return WorldBranchNode.create( newSubNodes ); 
	}
}
