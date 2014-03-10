package togos.networkrts.experimental.gensim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Updates a stepper at most once per minStepInterval.
 * If processing a tick takes longer than that, the simulation slows.
 * i.e. this will not try to 'catch up' to match real time.
 */
public class EventLooper<EventClass>
{
	/** Minimum number of milliseconds between steps. */
	final long minStepInterval;
	final RealTimeEventSource<EventClass> eventSource;
	final EventBuffer<EventClass> eBuf;
	
	protected AutoEventUpdatable2<EventClass> stepper;
	
	public EventLooper( RealTimeEventSource<EventClass> eventSource, AutoEventUpdatable2<EventClass> ticker, long minStepInterval ) {
		this.eventSource = eventSource;
		this.stepper = ticker;
		this.minStepInterval = minStepInterval;
		this.eBuf = new EventBuffer<EventClass>(eventSource.getCurrentTime());
	}
	
	public void run() throws Exception {
		long previousTickStartTime = eventSource.getCurrentTime() - minStepInterval / 2;
		while( eventSource.hasMoreEvents() || stepper.getNextAutoUpdateTime() < AutoEventUpdatable.TIME_INFINITY ) {
			final List<EventClass> incomingEvents;
			final long currentTick = stepper.getCurrentTime();
			final long currentTime = eventSource.getCurrentTime();
			final long nextAutoTick = stepper.getNextAutoUpdateTime();
			
			long nextAutoTickTime = previousTickStartTime + (nextAutoTick - currentTick) * minStepInterval;
			if( nextAutoTickTime < currentTime+1 ) {
				System.err.println("Simulation running slow!  Tick took "+(currentTime-previousTickStartTime)+" milliseconds");
				nextAutoTickTime = currentTime + 1;
			}
			
			final long nextTick, nextTickTime;
			eBuf.time = currentTime;
			if( eventSource.recv(nextAutoTickTime, eBuf) ) {
				incomingEvents = new ArrayList<EventClass>(8);
				incomingEvents.add(eBuf.payload);
				// Figure the next tick at which this event should be processed
				nextTick     = currentTick + (long)Math.ceil( (eBuf.time - currentTime)/(double)minStepInterval );
				nextTickTime = currentTime + (nextAutoTick - currentTick) * minStepInterval;
				while( eventSource.recv(nextTickTime, eBuf) ) {
					incomingEvents.add(eBuf.payload);
				}
			} else {
				nextTick     = nextAutoTick;
				nextTickTime = nextAutoTickTime;
				incomingEvents = Collections.<EventClass>emptyList();
			}
			
			previousTickStartTime = currentTime;
			stepper = stepper.update(nextTick, incomingEvents);
		}
	}
}