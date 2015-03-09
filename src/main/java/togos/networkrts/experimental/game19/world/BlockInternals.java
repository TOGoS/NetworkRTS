package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.sim.UpdateContext;
import togos.networkrts.util.BitAddressRange;

/**
 * The range returned by BitAddressRange does not need to include
 * the block's address.  Its purpose is to allow the behavior to
 * catch messages other than the ones directed to the block.
 */
public interface BlockInternals extends BitAddressRange, HasAutoUpdateTime
{
	public int getNonTileCollisionInteraction(
		Block b, long collisionTime, double x, double y, double w, double h, NonTile nt);
	
	// TODO: Replace action list with message list
	// TODO: May want to update parameter types to be more specific:
	// - take a (read-only) MessageCollection of incoming messages
	// - take a callback that accepts outgoing messages
	public Block update( Block b, int x, int y, int sizePower, long time, MessageSet messages, UpdateContext updateContext );
}
