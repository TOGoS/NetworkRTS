package togos.networkrts.experimental.gensim;

public class QueuelessRealTimeEventSource<EventClass> implements RealTimeEventSource<EventClass>
{
	long incomingEventTime;
	boolean closed;
	EventClass incomingEvent;
	
	@Override public synchronized boolean recv( long returnBy, EventBuffer<EventClass> buf ) throws InterruptedException {
		if( returnBy < buf.time ) returnBy = buf.time; // Ensure that -inf is treated as < +inf
		long waitTime;
		EventClass evt;
		while( (evt = incomingEvent) == null && !closed && (waitTime = returnBy - getCurrentTime()) > 0 ) wait( waitTime );
		if( evt == null ) {
			buf.time = returnBy;
			return false;
		}
		incomingEvent = null;
		notifyAll(); // Allow a new event to be posted!
		buf.update( evt, incomingEventTime, returnBy );
		return true;
	}
	
	@Override public long getCurrentTime() {
		return System.currentTimeMillis();
	}
	
	@Override public boolean hasMoreEvents() {
		return !closed;
	}
	
	public synchronized void post( EventClass evt ) throws InterruptedException {
		while( incomingEvent != null ) wait();
		incomingEventTime = getCurrentTime();
		incomingEvent     = evt;
		notifyAll();
	}
	
	/**
	 * Causes recv() to henceforth return immediately with no event.
	 */
	public synchronized void close() {
		closed = true;
		notifyAll();
	}
}
