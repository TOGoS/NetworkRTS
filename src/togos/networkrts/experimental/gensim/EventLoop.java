package togos.networkrts.experimental.gensim;

import togos.networkrts.util.Timer;
import togos.networkrts.experimental.gensim.EventBuffer;

public class EventLoop
{
	private EventLoop() { }
	
	// Style A
	public static <EventClass> void run( TimedEventQueue<EventClass> teq, TimedEventHandler<EventClass> eventHandler ) throws Exception {
		long curTime = Long.MIN_VALUE;
		while( !Thread.interrupted() ) {
			Timer<EventClass> evt = teq.take();
			// Real-time events may come in marked for the past;
			// in that case, do not rewind time
			if( evt.time > curTime ) curTime = evt.time; 
			eventHandler.update( curTime, evt.payload );
		}
	}
	
	// Style B
	// (terminates when eventSource has no more events and next internal update time = infinity)
	public static <EventClass> void run( RealTimeEventSource<EventClass> eventSource, Stepper<EventClass> stepper ) throws Exception {
		EventBuffer<EventClass> buf = new EventBuffer<EventClass>( eventSource.getCurrentTime() );
		while( eventSource.hasMoreEvents() || stepper.getNextInternalUpdateTime() != Stepper.TIME_INFINITY ) {
			boolean eventOccured = eventSource.recv( stepper.getNextInternalUpdateTime(), buf );
			stepper = stepper.update(buf.time, eventOccured ? buf.payload : null);
		}
	}
}
