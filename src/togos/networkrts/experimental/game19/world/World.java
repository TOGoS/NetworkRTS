package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.Layer.LayerLink;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityAggregation;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.util.BitAddressUtil;

public class World implements EntityAggregation
{
	public final RSTNode rst;
	public final int rstSizePower;
	public final EntitySpatialTreeIndex<NonTile> nonTiles;
	// May need a separate index for 'watchers'
	public final LayerLink background;
	
	public World(RSTNode rst, int rstSizePower, EntitySpatialTreeIndex<NonTile> nonTiles, LayerLink background ) {
		this.rst = rst;
		this.rstSizePower = rstSizePower;
		this.nonTiles = nonTiles.freeze();
		this.background = background;
	}
	
	public World(RSTNode rst, int rstSizePower, EntitySpatialTreeIndex<NonTile> nonTiles ) {
		this( rst, rstSizePower, nonTiles, null );
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
		return new World( rst, rstSizePower, nonTiles.with(nt), background );
	}
	
	// Could limit this to union of RST and node trees.
	@Override public AABB getAabb() { return AABB.BOUNDLESS; }
	
	@Override public long getMinBitAddress() {
		return BitAddressUtil.minAddress(rst.getMinBitAddress(), nonTiles.getMinBitAddress());
	}
	
	@Override public long getMaxBitAddress() {
		return BitAddressUtil.maxAddress(rst.getMaxBitAddress(), nonTiles.getMaxBitAddress());
	}
}
