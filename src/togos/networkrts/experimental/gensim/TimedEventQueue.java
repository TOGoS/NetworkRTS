package togos.networkrts.experimental.gensim;

import java.util.PriorityQueue;

import togos.networkrts.util.Timed;

public class TimedEventQueue<EventClass>
{
	protected final PriorityQueue<Timed<EventClass>> q = new PriorityQueue<Timed<EventClass>>( 128 );
	
	public synchronized Timed<EventClass> peek() {
		Timed<EventClass> next = q.peek();
		return (next != null && next.time <= System.currentTimeMillis() ) ? next : null;
	}
	
	public synchronized Timed<EventClass> take() throws InterruptedException {
		Timed<EventClass> next;
		long waitTime = -1; // Compiler thinks this needs to be initialized
		while( (next = q.peek()) == null || (waitTime = next.time - System.currentTimeMillis()) > 0 ) {
			if( next == null ) wait();
			else wait( waitTime );
		}
		q.remove();
		return next;
	}
	
	public synchronized void enqueue( Timed<EventClass> elem ) {
		q.add( elem );
		notifyAll();
	}
	
	public void enqueueImmediate( EventClass evt ) {
		enqueue( new Timed(System.currentTimeMillis(),evt) );
	}
	
	public void enqueue( long executeAt, EventClass evt ) {
		enqueue( new Timed(executeAt,evt) );
	}
}
