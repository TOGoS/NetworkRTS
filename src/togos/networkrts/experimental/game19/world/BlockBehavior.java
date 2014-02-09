package togos.networkrts.experimental.game19.world;

import java.util.List;

import togos.networkrts.experimental.game18.sim.IDUtil;

public interface BlockBehavior
{
	class NoBehavior implements BlockBehavior {
		private NoBehavior() { }
		@Override public long getMinId() { return IDUtil.MAX_ID; }
		@Override public long getMaxId() { return IDUtil.MIN_ID; }
		@Override public long getNextAutoUpdateTime() { return Long.MAX_VALUE; }
		@Override public Block update( Block b, int x, int y, int size, long time, Message[] messages, List<Action> results ) {
			return b;
		}
	}
	
	public static final BlockBehavior NONE = new NoBehavior(); 
	
	public long getMinId();
	public long getMaxId();
	public long getNextAutoUpdateTime(); 
	public Block update( Block b, int x, int y, int size, long time, Message[] messages, List<Action> results );
}
