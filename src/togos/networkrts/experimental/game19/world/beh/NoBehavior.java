package togos.networkrts.experimental.game19.world.beh;

import java.util.List;

import togos.networkrts.experimental.game19.world.Action;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockBehavior;
import togos.networkrts.experimental.game19.world.IDs;
import togos.networkrts.experimental.game19.world.Message;

public class NoBehavior implements BlockBehavior
{
	public static final NoBehavior instance = new NoBehavior();
	
	private NoBehavior() { }
	@Override public long getMinId() { return IDs.GENERIC_BLOCK_ID; }
	@Override public long getMaxId() { return IDs.GENERIC_BLOCK_ID; }
	@Override public long getNextAutoUpdateTime() { return Long.MAX_VALUE; }
	@Override public Block update( Block b, int x, int y, int sizePower, long time, Message[] messages, List<Action> results ) {
		return b;
	}
}
