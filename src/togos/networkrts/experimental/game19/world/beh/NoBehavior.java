package togos.networkrts.experimental.game19.world.beh;

import togos.networkrts.experimental.game19.sim.UpdateContext;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockBehavior;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.util.BitAddressUtil;

public class NoBehavior implements BlockBehavior
{
	public static final NoBehavior instance = new NoBehavior();
	
	private NoBehavior() { }
	@Override public long getMinBitAddress() { return BitAddressUtil.MAX_ADDRESS; }
	@Override public long getMaxBitAddress() { return BitAddressUtil.MIN_ADDRESS; }
	@Override public long getNextAutoUpdateTime() { return Long.MAX_VALUE; }
	@Override public Block update( Block b, int x, int y, int sizePower, long time, MessageSet messages, UpdateContext updateContext ) {
		return b;
	}
}
