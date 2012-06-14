package togos.networkrts.experimental.entree2;

import togos.networkrts.experimental.entree.ClipShape;
import togos.networkrts.experimental.netsim2.Sink;

public interface Entree
{
	/**
	 * Apply the given updates and return the new world.
	 * The update array may be re-ordered as a side-effect.
	 */
	public Entree update( WorldObjectUpdate[] updates, int off, int len );
	public void forEachObject( long requireFlags, long maxAutoUpdateTime, ClipShape clip, Sink<WorldObject> callback ) throws Exception;
	public int getObjectCount();
	public long getAllFlags();
	public long getMinAutoUpdateTime();
}
