package togos.networkrts.experimental.gensim;

import java.util.PriorityQueue;

import togos.networkrts.util.Timer;

public abstract class BaseMutableAutoUpdatable<EventClass> implements AutoEventUpdatable<EventClass>
{
	protected long currentTime = Long.MIN_VALUE;
	private PriorityQueue<Timer<EventClass>> timerQueue = new PriorityQueue<Timer<EventClass>>();
	
	//// Stepper implementation
	
	@Override public long getNextAutoUpdateTime() {
		Timer<EventClass> t = timerQueue.peek();
		return t == null ? Long.MAX_VALUE : t.time;
	}
	
	@Override public final BaseMutableAutoUpdatable<EventClass> update( long targetTime, EventClass evt ) throws Exception {
		if( targetTime < currentTime ) {
			throw new RuntimeException("Tried to rewind time from "+currentTime+" to "+targetTime);
		}
		
		Timer<EventClass> timer;
		while( (timer = timerQueue.peek()) != null && timer.time <= currentTime ) {
			timerQueue.remove();
			_update( timer.time > currentTime ? timer.time : currentTime, timer.payload );
		}
		
		_update( targetTime, evt );
		return this;
	}
	
	private final void _update( long targetTime, EventClass evt ) {
		passTime( targetTime );
		assert currentTime == targetTime;
		if( evt != null ) handleEvent( evt );
	}
	
	//// For use internally
	
	protected final void schedule( long time, EventClass evt ) {
		timerQueue.add(new Timer<EventClass>(time, evt));
	}
	
	protected final int getTimerCount() {
		return timerQueue.size();
	}
	
	protected final long getCurrentTime() {
		return currentTime;
	}
	
	//// Override these
	
	/**
	 * Simulate time passing from currentTime to targetTime.
	 * This method may modify currentTime during execution and must
	 * return with currentTime = targetTime.
	 * 
	 * This is always called at least once per update(...) even
	 * if no time actually passed.
	 */
	protected void passTime( long targetTime ) {
		this.currentTime = targetTime;
	}
	
	/**
	 * Simulate the given event occurring at currentTime.
	 */
	protected void handleEvent( EventClass evt ) { }
}
