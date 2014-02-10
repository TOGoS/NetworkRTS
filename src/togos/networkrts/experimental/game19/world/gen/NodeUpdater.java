package togos.networkrts.experimental.game19.world.gen;

import togos.networkrts.experimental.game19.world.WorldNode;

public interface NodeUpdater
{
	public WorldNode update( WorldNode oldNode, int x, int y, int sizePower );
}
