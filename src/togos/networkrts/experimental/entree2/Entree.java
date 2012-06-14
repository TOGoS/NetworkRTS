package togos.networkrts.experimental.entree2;

import togos.networkrts.experimental.cshape.CShape;
import togos.networkrts.experimental.netsim2.Sink;

public interface Entree<WorldObjectClass extends WorldObject>
{
	/**
	 * Apply the given updates and return the new world.
	 * The update array may be re-ordered as a side-effect.
	 */
	public Entree update( WorldObjectUpdate<WorldObjectClass>[] updates, int off, int len );
	public void forEachObject( long requireFlags, long maxAutoUpdateTime, CShape clip, Sink<WorldObjectClass> callback ) throws Exception;
	public int getObjectCount();
	public long getAllFlags();
	public long getMinAutoUpdateTime();
}
