package togos.networkrts.experimental.game19.world;

import java.util.List;

public interface BlockBehavior
{
	public long getMinId();
	public long getMaxId();
	public long getNextAutoUpdateTime(); 
	public Block update( Block b, int x, int y, int sizePower, long time, Message[] messages, List<Action> results );
}
