package togos.networkrts.experimental.game19.world.beh;

import java.util.List;

import togos.networkrts.experimental.game19.world.Action;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockBehavior;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.util.BitAddressUtil;

public class NoBehavior implements BlockBehavior
{
	public static final NoBehavior instance = new NoBehavior();
	
	private NoBehavior() { }
	@Override public long getMinBitAddress() { return BitAddressUtil.MAX_ADDRESS; }
	@Override public long getMaxBitAddress() { return BitAddressUtil.MIN_ADDRESS; }
	@Override public long getNextAutoUpdateTime() { return Long.MAX_VALUE; }
	@Override public Block update( Block b, int x, int y, int sizePower, long time, Message[] messages, List<Action> results ) {
		return b;
	}
}
