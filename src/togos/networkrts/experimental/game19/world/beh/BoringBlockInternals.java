package togos.networkrts.experimental.game19.world.beh;

import togos.networkrts.experimental.game19.sim.UpdateContext;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockInternals;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.util.BitAddressUtil;

/**
 * It just sits there!
 */
public class BoringBlockInternals implements BlockInternals
{
	public static final BoringBlockInternals INSTANCE = new BoringBlockInternals();
	
	private BoringBlockInternals() { }
	@Override public long getMinBitAddress() { return BitAddressUtil.MAX_ADDRESS; }
	@Override public long getMaxBitAddress() { return BitAddressUtil.MIN_ADDRESS; }
	@Override public long getNextAutoUpdateTime() { return Long.MAX_VALUE; }
	@Override
	public int getNonTileCollisionInteraction(
		Block b, long collisionTime, double x, double y, double w, double h, NonTile nt
	) {
		return (b.flags & Block.FLAG_SOLID) == 0 ? Block.INTERACTION_SA_PASS : Block.INTERACTION_SA_BOUNCE;
	}
	@Override public Block update( Block b, int x, int y, int sizePower, long time, MessageSet messages, UpdateContext updateContext ) {
		return b;
	}
}
