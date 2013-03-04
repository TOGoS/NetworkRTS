package togos.networkrts.experimental.asyncjava.interp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;

import togos.networkrts.experimental.asyncjava.Callback;
import togos.networkrts.experimental.asyncjava.ProgramSegment;
import togos.networkrts.experimental.asyncjava.ProgramShell;
import togos.networkrts.experimental.asyncjava.Puller;

public class ProgramRunner implements Runnable, ProgramShell
{
	static final class PullerState<DataType> {
		public final Puller<DataType> puller;
		public final Collection<Callback<DataType>> callbacks;
		public Thread thread;
		public boolean stopped;
		
		public PullerState( Puller<DataType> r ) {
			this.puller = r;
			this.callbacks = new ArrayList<Callback<DataType>>();
		}
	}
	
	static final class SegmentTimer implements Comparable<SegmentTimer> {
		final long timestamp;
		final long order;
		final ProgramSegment seg;
		public SegmentTimer( long ts, long order, ProgramSegment seg ) {
			this.timestamp = ts;
			this.seg       = seg;
			this.order     = order;
		}
		@Override public int compareTo(SegmentTimer o) {
			return timestamp < o.timestamp ? -1 : timestamp > o.timestamp ? 1 :
				order < o.order ? -1 : order > o.order ? 1 : 0;
		}
	}
	
	LinkedList<ProgramSegment> immediatelyScheduledSegments = new LinkedList<ProgramSegment>();
	/** State information about active or not-yet-started pullers. */
	HashMap<Puller,PullerState> pullerStates = new HashMap<Puller,PullerState>();
	
	PriorityQueue<SegmentTimer> timedSegments = new PriorityQueue<SegmentTimer>();
	
	Thread mainThread = null;
	protected synchronized boolean isMainThread() {
		if( mainThread == null ) mainThread = Thread.currentThread();
		return mainThread == Thread.currentThread();
	}
	
	protected <T> PullerState<T> getEventTriggers( Puller<T> puller ) {
		assert isMainThread();
		
		PullerState state = pullerStates.get(puller);
		if( state == null ) {
			state = new PullerState(puller);
			pullerStates.put(puller, state);
		}
		return state;
	}
	
	private int activePullerCount = 0;
	
	public <T> void start( final Puller<T> puller ) {
		assert isMainThread();
		
		final PullerState state = pullerStates.get(puller);
		if( state.thread != null ) return;
		
		Thread t = new Thread() {
			public void run() {
				Exception error = null;
				T data;
				try {
					data = puller.pull();
				} catch( Exception e ) {
					data = null;
					error = e;
				}
				while( true ) {
					final T _data = data;
					final Exception _error = error;
					scheduleAsync(new ProgramSegment() {
						@Override
						public void run(ProgramShell shell) {
							if( state.stopped ) return;
							for( Callback<T> t : getEventTriggers(puller).callbacks ) {
								shell.schedule(t.onData(_error, _data));
							}
							if( _data == null ) {
								pullerStates.remove(puller);
								--activePullerCount;
							}
						}
					});
					
					if( data == null ) break;
					
					try {
						data = puller.pull();
					} catch( Exception e ) {
						System.err.println("Exception while pulling: "+e);
						data = null;
						error = e;
					}
				};
			}
		};
		t.start();
		++activePullerCount;
	}
	
	public <T> void stop( final Puller<T> puller ) {
		assert isMainThread();
		
		PullerState ps = pullerStates.get(puller);
		if( ps == null ) return;
		
		ps.puller.stop();
		ps.stopped = true;
		pullerStates.remove(puller);
		--activePullerCount;
	}
	
	////
	
	/** 
	 * Schedule a segment to be executed after all immediately
	 * scheduled things are done.  This method may be called from any thread.
	 */
	public void scheduleAsync( ProgramSegment seg ) {
		try {
			pushIncomingSegment(seg);
		} catch( InterruptedException e ) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}
	
	public long getCurrentTime() {
		assert isMainThread();
		return System.currentTimeMillis();
	}
	
	@Override public void schedule( ProgramSegment seg ) {
		assert isMainThread();
		immediatelyScheduledSegments.add(0, seg);
	}
	
	protected long order = 0;
	@Override public void schedule( final long timestamp, final ProgramSegment seg ) {
		assert isMainThread();
		timedSegments.add( new SegmentTimer( timestamp, order++, seg ) );
	}
	
	@Override public <DataType> void onData(Puller<DataType> input, Callback<DataType> trigger) {
		assert isMainThread();
		getEventTriggers(input).callbacks.add(trigger);
	}
	
	protected ProgramSegment incomingSegment = null;
	
	protected synchronized void pushIncomingSegment( ProgramSegment seg ) throws InterruptedException {
		while( incomingSegment != null ) wait();
		incomingSegment = seg;
		notifyAll();
	}
	protected synchronized ProgramSegment pullIncomingSegment( long until ) throws InterruptedException {
		assert isMainThread();
		long ct = getCurrentTime();
		while( incomingSegment == null && ct < until ) {
			wait( until - ct );
			ct = System.currentTimeMillis();
		}
		ProgramSegment seg = incomingSegment;
		incomingSegment = null;
		notify(); // Since all others waiting should be pushers, only need to notify one.
		return seg;
	}
	
	@Override public void run() {
		assert isMainThread();
		while( immediatelyScheduledSegments.size() > 0 || timedSegments.size() > 0 || activePullerCount > 0 ) {
			while( immediatelyScheduledSegments.size() > 0 ) {
				immediatelyScheduledSegments.remove().run(this);
			}
			
			long nextTimer;
			SegmentTimer segTimer = timedSegments.peek();
			nextTimer = (segTimer != null) ? segTimer.timestamp : Long.MAX_VALUE;
			if( segTimer.timestamp <= getCurrentTime() ) {
				timedSegments.remove();
				System.err.println("Got a timed segment for "+segTimer.timestamp);
				segTimer.seg.run(this);
			} else if( activePullerCount > 0 )  {
				ProgramSegment inSeg;
				try {
					inSeg = pullIncomingSegment( nextTimer );
				} catch( InterruptedException e ) {
					Thread.currentThread().interrupt();
					e.printStackTrace();
					return;
				}
				if( inSeg != null ) {
					inSeg.run(this);
				} else if( segTimer != null ) {
					timedSegments.remove();
					segTimer.seg.run(this);
				}
			}
		}
	}
}
