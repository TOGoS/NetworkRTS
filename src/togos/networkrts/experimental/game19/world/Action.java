package togos.networkrts.experimental.game19.world;

import java.util.List;

public interface Action
{
	public boolean appliesToWorldNode( WorldNode n, int x, int y, int size, long time );
	public WorldNode applyToWorldNode( WorldNode n, int x, int y, int size, long time, List<Action> results );
}
