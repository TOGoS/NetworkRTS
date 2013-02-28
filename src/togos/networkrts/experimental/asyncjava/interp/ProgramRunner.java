package togos.networkrts.experimental.asyncjava.interp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

import togos.networkrts.experimental.asyncjava.ProgramSegment;
import togos.networkrts.experimental.asyncjava.ProgramShell;
import togos.networkrts.experimental.asyncjava.Puller;
import togos.networkrts.experimental.asyncjava.Callback;

public class ProgramRunner implements Runnable, ProgramShell
{
	static class PullerState<DataType> {
		public final Puller<DataType> puller;
		public final Collection<Callback<DataType>> callbacks;
		public Thread thread;
		public boolean stopped;
		
		public PullerState( Puller<DataType> r ) {
			this.puller = r;
			this.callbacks = new ArrayList<Callback<DataType>>();
		}
	}
	
	LinkedList<ProgramSegment> immediatelyScheduledSegments = new LinkedList<ProgramSegment>();
	/** State information about active or not-yet-started pullers. */
	HashMap<Puller,PullerState> pullerStates = new HashMap<Puller,PullerState>();
	/** Queue of segments that should be run after immediatelyScheduledSegments is drained. */
	ArrayBlockingQueue<ProgramSegment> ioTriggeredSegments = new ArrayBlockingQueue<ProgramSegment>(10); 
	
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
		ioTriggeredSegments.add(seg);
	}
	
	public long getCurrentTime() {
		assert isMainThread();
		return System.currentTimeMillis();
	}
	
	@Override public void schedule( ProgramSegment seg ) {
		assert isMainThread();
		immediatelyScheduledSegments.add(0, seg);
	}
	
	@Override public void schedule( final long timestamp, final ProgramSegment seg ) {
		assert isMainThread();
		
		if( timestamp <= getCurrentTime() ) {
			scheduleAsync(seg);
			return;
		}
		
		// TODO: should have a single thread with a priority queue
		new Thread() {
			@Override
			public void run() {
				long cTime = System.currentTimeMillis();
				if( timestamp > cTime ) {
					try {
						Thread.sleep(timestamp-cTime);
					} catch( InterruptedException e ) {
						System.err.println("Interrupted while waiting for scheduled event");
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}
				}
				scheduleAsync(seg);
			}
		}.start();
	}
	
	@Override public <DataType> void onData(Puller<DataType> input, Callback<DataType> trigger) {
		assert isMainThread();
		getEventTriggers(input).callbacks.add(trigger);
	}
	
	@Override public void run() {
		assert isMainThread();
		while( immediatelyScheduledSegments.size() > 0 || activePullerCount > 0 || ioTriggeredSegments.size() > 0 ) {
			while( immediatelyScheduledSegments.size() > 0 ) {
				immediatelyScheduledSegments.remove().run(this);
			}
			if( ioTriggeredSegments.size() > 0 ) {
				ioTriggeredSegments.remove().run(this);
			}
		}		
	}
}
