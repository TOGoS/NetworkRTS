package togos.networkrts.experimental.gensim;

import java.util.Comparator;
import java.util.PriorityQueue;

public class TimedEventQueue<E extends Timestamped> implements Timekeeper
{
	protected long currentTimestamp;
	
	protected final PriorityQueue<E> q = new PriorityQueue<E>( 128, new Comparator<E>() {
		public int compare(E o1, E o2) {
			return o1.getTimestamp() < o2.getTimestamp() ? -1 : o1.getTimestamp() > o2.getTimestamp() ? 1 : 0;
		}
	});
	
	public synchronized E peek() {
		E next = q.peek();
		return (next != null && next.getTimestamp() <= currentTimestamp ) ? next : null;
	}
	
	public synchronized E take() throws InterruptedException {
		while( true ) {
			E next = q.peek();
			if( next != null ) {
				long waitAtMost = next.getTimestamp() - currentTimestamp;
				if( waitAtMost <= 0 ) return q.remove();
				this.wait();
			}
		}
	}
	
	public synchronized void add( E elem ) {
		q.add( elem );
		notifyAll();
	}
	
	public synchronized void advanceTimeTo( long timestamp ) {
		currentTimestamp = timestamp;
		notifyAll();
	}
	
	@Override public synchronized long getCurrentTimestamp() {
		return currentTimestamp;
	}
	
	@Override public synchronized void waitUntil(long timestamp) throws InterruptedException {
		while( timestamp < currentTimestamp ) wait();
	}
}
