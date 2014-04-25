package togos.networkrts.experimental.gensim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Updates a stepper at most once per minStepInterval.
 * If processing a tick takes longer than that, the simulation slows.
 * i.e. this will not try to 'catch up' to match real time.
 */
public class EventLooper<EventClass> extends Thread
{
	/** Minimum number of milliseconds between steps. */
	final long minStepInterval;
	final RealTimeEventSource<EventClass> eventSource;
	final EventBuffer<EventClass> eBuf;
	
	protected AutoEventUpdatable2<EventClass> stepper;
	public boolean reportSlowness = true;
	
	public EventLooper( String name, RealTimeEventSource<EventClass> eventSource, AutoEventUpdatable2<EventClass> ticker, long minStepInterval ) {
		super(name);
		assert minStepInterval > 0;
		
		this.eventSource = eventSource;
		this.stepper = ticker;
		this.minStepInterval = minStepInterval;
		this.eBuf = new EventBuffer<EventClass>(eventSource.getCurrentTime());
	}
	
	public EventLooper( RealTimeEventSource<EventClass> eventSource, AutoEventUpdatable2<EventClass> ticker, long minStepInterval ) {
		this("Event looper", eventSource, ticker, minStepInterval);
	}
	
	/*
	 * Ticks are treated as instants in time.
	 * Looper waits until a tick, then processes events and auto-updates
	 * that are supposed to occur at that tick, then repeats
	 * for the next tick.
	 * If processing takes a long time, the next tick will be delayed.
	 * Looper will not try to 'catch up'.
	 * Better to have a slow-motion simulation than an erratic one, I think.
	 * Looper will skip ticks when there is nothing going on
	 * (i.e. no incoming events and stepper.nextAutoUpdateTime() is more
	 * than one tick in the future).
	 *
	 *            +- previous tick
	 *            | +- processing interval
	 *            | |   +- waiting interval +- over-long processing interval
	 *            | |  _|___ +- next tick __|_________________ +- 1-ms delay
	 *            |/ \/     \|           /                    \|
	 * |pppppppp..|ppp.......|pp........|pppppppppppppppppppppp.|
	 * |          |          |          |                       |
	 * tick       tick       tick       tick                    tick
	 */
	
	public void _run() throws IOException, InterruptedException {
		long previousTickTime = eventSource.getCurrentTime() - minStepInterval / 2;
		while( eventSource.hasMoreEvents() || stepper.getNextAutoUpdateTime() < AutoEventUpdatable.TIME_INFINITY ) {
			final List<EventClass> incomingEvents;
			final long previousTick = stepper.getCurrentTime();
			final long waitStartTime = eventSource.getCurrentTime();
			final long nextAutoTick = stepper.getNextAutoUpdateTime();
			assert nextAutoTick > previousTick;
			
			long nextAutoTickTime = nextAutoTick == Long.MAX_VALUE ? Long.MAX_VALUE : previousTickTime + (nextAutoTick - previousTick) * minStepInterval;
			if( nextAutoTickTime < waitStartTime+1 ) {
				if( reportSlowness ) System.err.println("Simulation running slow!  Processing took "+(waitStartTime-previousTickTime)+" milliseconds");
				nextAutoTickTime = waitStartTime + 1;
			}
			
			// Next tick may come before next auto tick
			// due to incoming events.
			final long nextTick, nextTickTime;
			eBuf.time = waitStartTime;
			if( eventSource.recv(nextAutoTickTime, eBuf) ) {
				incomingEvents = new ArrayList<EventClass>(8);
				incomingEvents.add(eBuf.payload);
				// Figure the next tick at which this event should be processed
				nextTick     = previousTick + (long)Math.ceil( (eBuf.time - previousTickTime)/(double)minStepInterval );
				nextTickTime = previousTickTime + (nextTick - previousTick) * minStepInterval;
				while( eventSource.recv(nextTickTime, eBuf) ) {
					incomingEvents.add(eBuf.payload);
				}
			} else {
				nextTick     = nextAutoTick;
				nextTickTime = nextAutoTickTime;
				incomingEvents = Collections.<EventClass>emptyList();
			}
			
			assert nextTick < Long.MAX_VALUE;
			previousTickTime = nextTickTime;
			stepper = stepper.update(nextTick, incomingEvents);
		}
	}
	
	public void run() {
		try {
			_run();
		} catch( IOException e ) {
			System.err.println(getName()+" had IOException while reading incoming events:");
			e.printStackTrace();
			interrupt();
		} catch( InterruptedException e ) {
			System.err.println(getName()+" interrupted while waiting for incoming events");
			interrupt();
		}
	}
}
