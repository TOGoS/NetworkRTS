package togos.networkrts.experimental.asyncjava.interp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import togos.networkrts.experimental.asyncjava.Callback;
import togos.networkrts.experimental.asyncjava.ProgramSegment;
import togos.networkrts.experimental.asyncjava.ProgramShell;
import togos.networkrts.experimental.asyncjava.Puller;
import togos.networkrts.experimental.gensim.BaseMutableStepper;
import togos.networkrts.experimental.gensim.EventLoop;
import togos.networkrts.experimental.gensim.QueuelessRealTimeEventSource;

public class ProgramRunner extends BaseMutableStepper<ProgramSegment> implements Runnable
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
		
	LinkedList<ProgramSegment> immediatelyScheduledSegments = new LinkedList<ProgramSegment>();
	/** State information about active or not-yet-started pullers. */
	HashMap<Puller,PullerState> pullerStates = new HashMap<Puller,PullerState>();
	QueuelessRealTimeEventSource<ProgramSegment> inputEventSource = new QueuelessRealTimeEventSource<ProgramSegment>() {
		public boolean hasMoreEvents() {
			return super.hasMoreEvents() && activePullerCount > 0 && getNextInternalUpdateTime() != TIME_INFINITY;
		}
	};
	ProgramShell shell = new ProgramShell() {
		public long getCurrentTime() { return ProgramRunner.this.getCurrentTime(); };
		public <DataType> void onData( Puller<DataType> puller, Callback<DataType> trigger ) { ProgramRunner.this.onData( puller, trigger ); };
		public void schedule(ProgramSegment seg) { ProgramRunner.this.schedule( seg ); };
		public void schedule(long timestamp, ProgramSegment seg) { ProgramRunner.this.schedule( timestamp, seg ); };
		public <DataType> void start( Puller<DataType> puller ) { ProgramRunner.this.start( puller ); }
		public <DataType> void stop( Puller<DataType> puller ) { ProgramRunner.this.stop( puller ); }
	};
	
	Thread mainThread = null;
	protected synchronized boolean isMainThread() {
		if( mainThread == null ) mainThread = Thread.currentThread();
		return mainThread == Thread.currentThread();
	}
	
	//// Puller management stuff ////
	
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
			inputEventSource.post(seg);
		} catch( InterruptedException e ) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}
	
	@Override public long getNextInternalUpdateTime() {
		return immediatelyScheduledSegments.size() > 0 ? getCurrentTime() : super.getNextInternalUpdateTime();
	}
	
	public void schedule( ProgramSegment seg ) {
		assert isMainThread();
		immediatelyScheduledSegments.add(0, seg);
	}
	
	public <DataType> void onData(Puller<DataType> input, Callback<DataType> trigger) {
		assert isMainThread();
		getEventTriggers(input).callbacks.add(trigger);
	}
	
	////
	
	protected void runImmediateSegments() {
		while( immediatelyScheduledSegments.size() > 0 ) {
			immediatelyScheduledSegments.remove().run( shell );
		}
	}
	
	@Override protected void passTime(long currentTime, long targetTime) {
		runImmediateSegments();
	}
	
	@Override protected void handleEvent(ProgramSegment evt) {
		evt.run( shell );
		runImmediateSegments();
	}
	
	////
	
	@Override public void run() {
		assert isMainThread();
		
		try {
			EventLoop.run(inputEventSource, this);
		} catch( InterruptedException e ) {
			Thread.currentThread().interrupt();
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}
}
