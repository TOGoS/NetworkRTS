package togos.networkrts.experimental.gensim;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class QueuedRealTimeEventSource<EventClass> implements RealTimeEventSource<EventClass>
{
	protected boolean closed = false;
	public final LinkedBlockingQueue<EventClass> eventQueue = new LinkedBlockingQueue<EventClass>();
	
	@Override public boolean recv( long returnBy, EventBuffer<EventClass> buf ) throws InterruptedException {
		if( returnBy < buf.time ) returnBy = buf.time; // Ensure that -inf is treated as < +inf
		long waitTime = returnBy - getCurrentTime();
		if( !closed && waitTime > 0 ) {
			EventClass evt = eventQueue.poll(waitTime, TimeUnit.MILLISECONDS);
			if( !closed && evt != null ) {
				return buf.update( evt, System.currentTimeMillis(), returnBy );
			}
		}
		return buf.update( null, returnBy, returnBy );
	}
	
	@Override public long getCurrentTime() {
		return System.currentTimeMillis();
	}
	
	@Override public boolean hasMoreEvents() {
		return !closed;
	}
	
	/**
	 * Causes recv() to henceforth return immediately with no event.
	 */
	public synchronized void close() {
		closed = true;
		// TODO: put some value into the queue so that recv returns?
		notifyAll();
	}
}
