package togos.networkrts.experimental.gensim;

public class QueuelessRealTimeEventSource<EventClass> implements RealTimeEventSource<EventClass> {
	@Override public synchronized EventClass recv( long returnBy ) throws InterruptedException {
		long waitTime;
		EventClass evt;
		while( (evt = incomingEvent) == null && (waitTime = returnBy - getCurrentTime()) > 0 ) wait( waitTime );
		incomingEvent = null;
		notifyAll();
		return evt;
	}
	
	@Override public long getCurrentTime() {
		return System.currentTimeMillis();
	}
	
	EventClass incomingEvent;
	
	public synchronized void post( EventClass evt ) throws InterruptedException {
		while( incomingEvent != null ) wait();
		incomingEvent = evt;
		notifyAll();
	}
}
