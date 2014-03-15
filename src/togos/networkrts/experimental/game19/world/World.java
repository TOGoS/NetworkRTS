package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.util.HasNextAutoUpdateTime;

public class World implements HasNextAutoUpdateTime
{
	public final RSTNode rst;
	public final int rstSizePower;
	public final EntitySpatialTreeIndex<NonTile> nonTiles;
	// May need a separate index for 'watchers'
	
	public World(RSTNode rst, int rstSizePower, EntitySpatialTreeIndex<NonTile> nonTiles ) {
		this.rst = rst;
		this.rstSizePower = rstSizePower;
		this.nonTiles = nonTiles.freeze();
	}
	
	public RSTNodeInstance getRstNodeInstance() {
		return new RSTNodeInstance() {
			@Override public RSTNode getNode() { return rst; }
			@Override public int getNodeX() { return -(1<<(rstSizePower-1)); }
			@Override public int getNodeY() { return -(1<<(rstSizePower-1)); }
			@Override public int getNodeSizePower() { return rstSizePower; }
		};
	}
	
	public long getNextAutoUpdateTime() {
		return Math.min( rst.getNextAutoUpdateTime(), nonTiles.getNextAutoUpdateTime() );
	}
	
	//// Some convenience method
	
	public World withNonTile(NonTile nt) {
		return new World( rst, rstSizePower, nonTiles.with(nt) );
	}
}
