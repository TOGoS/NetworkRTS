package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;

public class World
{
	public final RSTNode rst;
	public final int rstSizePower;
	public final EntitySpatialTreeIndex<NonTile> entities;
	// May need a separate index for 'watchers'
	
	public World(RSTNode rst, int rstSizePower, EntitySpatialTreeIndex<NonTile> entities ) {
		this.rst = rst;
		this.rstSizePower = rstSizePower;
		this.entities = entities;
	}
	
	public RSTNodeInstance getRstNodeInstance() {
		return new RSTNodeInstance() {
			@Override public RSTNode getNode() { return rst; }
			@Override public int getNodeX() { return -(1<<(rstSizePower-1)); }
			@Override public int getNodeY() { return -(1<<(rstSizePower-1)); }
			@Override public int getNodeSizePower() { return rstSizePower; }
		};
	}
}
