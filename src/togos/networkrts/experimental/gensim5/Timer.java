package togos.networkrts.experimental.gensim5;

/**
 * For carrying events through a priority queue.
 * time gives the time the event should fire at.
 * order provides explicit sub-timestamp ordering.
 */
public class Timer<PayloadClass> implements Comparable<Timer>
{
	static long _nextOrder = 0;
	synchronized static long nextOrder() {
		return _nextOrder++;
	}
	
	public final long time;
	public final long order;
	public final PayloadClass payload;
	
	public Timer( long executeAt, long order, PayloadClass payload ) {
		this.time = executeAt;
		this.order = order;
		this.payload = payload;
	}
	
	public Timer( long time, PayloadClass payload ) {
		this( time, nextOrder(), payload );
	}

	@Override
	public int compareTo( Timer t ) {
		return time < t.time ? -1 : time > t.time ? 1 : order < t.order ? -1 : order > t.order ? 1 : 0;
	}
}
