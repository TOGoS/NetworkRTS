package togos.networkrts.experimental.entree2;

import togos.networkrts.experimental.entree.ClipShape;
import togos.networkrts.experimental.netsim2.Sink;

public class QuadEntree<WorldObjectClass extends WorldObject> implements Entree<WorldObjectClass>
{
	public final double x, y, w, h;
	public final QuadEntreeNode root;
	public final int maxSubdivision;
	
	public QuadEntree( double x, double y, double w, double h, QuadEntreeNode root, int maxSubdivision ) {
		this.x = x; this.y = y;
		this.w = w; this.h = h;
		this.root = root;
		this.maxSubdivision = maxSubdivision;
	}
	
	public QuadEntree update( QuadEntreeNode root ) {
		return root == this.root ? this : new QuadEntree( x, y, w, h, root, maxSubdivision );
	}
	
	@Override
	public Entree update( WorldObjectUpdate<WorldObjectClass>[] updates, int off, int count ) {
		boolean[] cats = new boolean[updates.length];
		WorldObject[] scratch = new WorldObject[updates.length];
		return update( root.update(updates, cats, scratch, off, off+count, x, y, w, h, maxSubdivision) );
	}
	
	@Override
	public void forEachObject(long flags, long autoUpdateTime, ClipShape s, Sink<WorldObjectClass> callback)
		throws Exception
	{
		root.forEachObject(flags, autoUpdateTime, s, callback, x, y, w, h);
	}
	
	@Override public long getMinAutoUpdateTime() { return root.minAutoUpdateTime; }
	@Override public long getAllFlags() { return root.allFlags; }
	@Override public int getObjectCount() { return root.objectCount; }
}
