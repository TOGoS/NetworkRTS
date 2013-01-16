package togos.networkrts.experimental.spacegame;

import java.util.ArrayList;
import java.util.List;

import togos.networkrts.experimental.gensim.Stepper;

public class SpaceWorld implements Stepper<SpaceWorld.SpaceWorldEvent>
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
	long time = 0;
	
	@Override public long getNextInternalUpdateTime() {
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
				o.behavior.receivedSignal(time, o, evt.code, evt.data, evt.offset, evt.length, newObjects, enqueuedEvents);
			} else {
				newObjects.add(o);
			}
		}
		objects = newObjects.toArray(new SpaceObject[newObjects.size()]);
	}
	
	protected void processTimedUpdates() {
		ArrayList<SpaceObject> newObjects = new ArrayList();
		for( SpaceObject o : objects ) {
			if( o.autoUpdateTime <= time ) {
				o.behavior.receivedSignal(time, o, SIGNAL_TIME, null, 0, 0, newObjects, enqueuedEvents);
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
	
	@Override public void handleEvent(SpaceWorldEvent evt) throws Exception {
		enqueuedEvents.add(evt);
		processEnqueuedEvents();
	}
	
	@Override public void setCurrentTime(long targetTime) throws Exception {
		long nextUpdateTime;
		while( (nextUpdateTime = getNextInternalUpdateTime()) <= targetTime ) {
			time = nextUpdateTime;
			processTimedUpdates();
			processEnqueuedEvents();
		}
		time = targetTime;
	}
}
