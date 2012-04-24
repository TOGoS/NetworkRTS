package togos.networkrts.experimental.netsim1;

import java.util.Comparator;
import java.util.PriorityQueue;

public class TimedEventQueue<E extends Timestamped>
{
	protected final PriorityQueue<E> q = new PriorityQueue<E>( 128, new Comparator<E>() {
		public int compare(E o1, E o2) {
			return o1.getTimestamp() < o2.getTimestamp() ? -1 : o2.getTimestamp() > o1.getTimestamp() ? 1 : 0;
		}
	});
	
	public synchronized E take() throws InterruptedException {
		while( true ) {
			E next = q.peek();
			if( next == null ) {
				this.wait();
			} else {
				long waitAtMost = next.getTimestamp() - System.currentTimeMillis();
				if( waitAtMost <= 0 ) {
					return q.remove();
				}
				this.wait( waitAtMost );
			}
		}
	}
	
	public synchronized void add( E elem ) {
		q.add( elem );
		notifyAll();
	}
}
