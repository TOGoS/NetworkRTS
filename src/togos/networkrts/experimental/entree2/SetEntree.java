package togos.networkrts.experimental.entree2;

import java.util.HashSet;
import java.util.Set;

import togos.networkrts.experimental.entree.ClipShape;
import togos.networkrts.experimental.netsim2.Sink;

public class SetEntree<WorldObjectClass extends WorldObject> implements Entree<WorldObjectClass>
{
	public final Set<WorldObjectClass> objects;
	public final long allFlags;
	public final long minAutoUpdateTime;
	
	public SetEntree( Set<WorldObjectClass> objects ) {
		this.objects = objects;
		long allFlags = 0;
		long minAutoUpdateTime = Long.MAX_VALUE;
		for( WorldObject o : objects ) {
			allFlags |= o.getFlags();
			minAutoUpdateTime = Math.min(minAutoUpdateTime, o.getAutoUpdateTime());
		}
		this.allFlags = allFlags;
		this.minAutoUpdateTime = minAutoUpdateTime;
	}

	@Override
	public Entree update(WorldObjectUpdate<WorldObjectClass>[] updates, int off, int len) {
		HashSet<WorldObjectClass> newObjects = new HashSet<WorldObjectClass>(objects);
		int end = off+len;
		for( int i=off; i<end; ++i ) {
			if( updates[i].isAddition ) newObjects.add( updates[i].worldObject );
			else newObjects.remove( updates[i].worldObject );
		}
		return new SetEntree(newObjects);
	}

	@Override
	public void forEachObject(long requireFlags, long maxAutoUpdateTime, ClipShape clip, Sink<WorldObjectClass> callback) throws Exception {
		for( WorldObjectClass o : objects ) {
			if( !clip.intersectsRect(o.x - o.maxRadius, o.y - o.maxRadius, o.maxRadius*2, o.maxRadius*2) ) continue;
			if( (o.getFlags() & requireFlags) != requireFlags ) continue;
			if( o.getAutoUpdateTime() > maxAutoUpdateTime ) continue;
			
			callback.give( o );
		}		
	}
	
	@Override public int getObjectCount() { return objects.size(); }
	@Override public long getAllFlags() { return allFlags; }
	@Override public long getMinAutoUpdateTime() { return minAutoUpdateTime; }
}
