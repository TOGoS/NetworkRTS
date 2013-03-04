package togos.networkrts.experimental.spacegame;

import java.util.ArrayList;
import java.util.List;

import togos.networkrts.experimental.gensim.AutoEventUpdatable;

public class SpaceWorld implements AutoEventUpdatable<SpaceWorld.SpaceWorldEvent>
{
	public final int SIGNAL_NONE = 0;
	public final int SIGNAL_TIME = 1;
	
	public static class SpaceWorldEvent {
		public final long targetId;
		public final int code;
		public final byte[] data;
		public final int offset, length;
		
		public SpaceWorldEvent( long targetId, int code, byte[] data, int off, int len ) {
			this.targetId = targetId;
			this.code = code;
			this.data = data;
			this.offset = off;
			this.length = len;
		}
	}
	
	SpaceObject[] objects = new SpaceObject[0];
	List<SpaceWorldEvent> enqueuedEvents;
	long currentTime = 0;
	
	@Override public long getNextAutomaticUpdateTime() {
		long t = Long.MAX_VALUE;
		for( SpaceObject o : objects ) {
			if( o.autoUpdateTime < t ) t = o.autoUpdateTime;
		}
		return t;
	}
	
	protected void processEvent(SpaceWorldEvent evt) {
		ArrayList<SpaceObject> newObjects = new ArrayList();
		for( SpaceObject o : objects ) {
			if( o.entityId == evt.targetId ) {
				o.behavior.receivedSignal(currentTime, o, evt.code, evt.data, evt.offset, evt.length, newObjects, enqueuedEvents);
			} else {
				newObjects.add(o);
			}
		}
		objects = newObjects.toArray(new SpaceObject[newObjects.size()]);
	}
	
	protected void processTimedUpdates() {
		ArrayList<SpaceObject> newObjects = new ArrayList();
		for( SpaceObject o : objects ) {
			if( o.autoUpdateTime <= currentTime ) {
				o.behavior.receivedSignal(currentTime, o, SIGNAL_TIME, null, 0, 0, newObjects, enqueuedEvents);
			} else {
				newObjects.add(o);
			}
		}
		objects = newObjects.toArray(new SpaceObject[newObjects.size()]);
	}
	
	protected void processEnqueuedEvents() {
		while( enqueuedEvents.size() > 0 ) {
			processEvent( enqueuedEvents.remove(0) );
		}
	}
	
	@Override public SpaceWorld update(long targetTime, SpaceWorldEvent evt) throws Exception {
		if( targetTime < currentTime ) {
			throw new RuntimeException("Tried to rewind time from "+currentTime+" to "+targetTime);
		}
		
		long nextUpdateTime;
		while( (nextUpdateTime = getNextAutomaticUpdateTime()) <= targetTime ) {
			currentTime = nextUpdateTime;
			processTimedUpdates();
			processEnqueuedEvents();
		}
		currentTime = targetTime;

		enqueuedEvents.add(evt);
		processEnqueuedEvents();
		return this;
	}
}
