package togos.networkrts.experimental.gensim;

import java.util.PriorityQueue;

import togos.networkrts.util.Timer;

public class TimedEventQueue<EventClass>
{
	protected final PriorityQueue<Timer<EventClass>> q = new PriorityQueue<Timer<EventClass>>( 128 );
	
	public synchronized Timer<EventClass> peek() {
		Timer<EventClass> next = q.peek();
		return (next != null && next.time <= System.currentTimeMillis() ) ? next : null;
	}
	
	public synchronized Timer<EventClass> take() throws InterruptedException {
		Timer<EventClass> next;
		long waitTime = -1; // Compiler thinks this needs to be initialized
		while( (next = q.peek()) == null || (waitTime = next.time - System.currentTimeMillis()) > 0 ) {
			if( next == null ) wait();
			else wait( waitTime );
		}
		q.remove();
		return next;
	}
	
	public synchronized void enqueue( Timer<EventClass> elem ) {
		q.add( elem );
		notifyAll();
	}
	
	public void enqueueImmediate( EventClass evt ) {
		enqueue( new Timer(System.currentTimeMillis(),evt) );
	}
	
	public void enqueue( long executeAt, long code, EventClass evt ) {
		enqueue( new Timer(executeAt,code,evt) );
	}
	
	public void enqueue( long executeAt, EventClass evt ) {
		enqueue( executeAt, 0, evt );
	}
}
