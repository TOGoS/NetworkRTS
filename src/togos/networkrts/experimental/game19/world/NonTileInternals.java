package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.gameengine1.index.AABB;

/**
 * Represents the internal state and behavior of a BlargNonTile.
 */
public interface NonTileInternals<NT extends BlargNonTile>
{
	public NonTile update( NT nt, long time, World w, MessageSet messages, NonTileUpdateContext updateContext );
	public Icon getIcon();
	public AABB getRelativePhysicalAabb();
	public long getNonTileAddressFlags();
	public long getNextAutoUpdateTime();
}
