package togos.networkrts.experimental.gensim;

public class QueuelessRealTimeEventSource<EventClass> implements RealTimeEventSource<EventClass>
{
	static long clamp( long min, long v, long max ) {
		return v < min ? min : v > max ? max : v;
	}
	
	@Override public synchronized boolean recv( long returnBy, EventBuffer buf ) throws InterruptedException {
		long prevTime = buf.time;
		long waitTime;
		EventClass evt;
		while( (evt = incomingEvent) == null && (waitTime = returnBy - getCurrentTime()) > 0 ) wait( waitTime );
		if( evt == null ) {
			buf.time = returnBy;
			return false;
		}
		incomingEvent = null;
		notifyAll(); // Allow a new event to be posted!
		buf.time = clamp( prevTime, incomingEventTime, returnBy );
		buf.data = evt;
		return true;
	}
	
	@Override public long getCurrentTime() {
		return System.currentTimeMillis();
	}
	
	long incomingEventTime;
	EventClass incomingEvent;
	
	public synchronized void post( EventClass evt ) throws InterruptedException {
		while( incomingEvent != null ) wait();
		incomingEventTime = getCurrentTime();
		incomingEvent     = evt;
		notifyAll();
	}
}
