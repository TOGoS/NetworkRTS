package togos.networkrts.experimental.gensim;

import togos.networkrts.util.Timed;
import togos.networkrts.experimental.gensim.EventBuffer;

public class EventLoop
{
	private EventLoop() { }
	
	// Style A
	public static <EventClass> void run( TimedEventQueue<EventClass> teq, TimedEventHandler<EventClass> eventHandler ) throws Exception {
		long curTime = Long.MIN_VALUE;
		while( !Thread.interrupted() ) {
			Timed<EventClass> evt = teq.take();
			// Real-time events may come in marked for the past;
			// in that case, do not rewind time
			if( evt.time > curTime ) curTime = evt.time; 
			eventHandler.update( curTime, evt.payload );
		}
	}
	
	// Style B
	public static <EventClass> void run( RealTimeEventSource<EventClass> es, Stepper<EventClass> stepper ) throws Exception {
		EventBuffer<EventClass> buf = new EventBuffer<EventClass>( es.getCurrentTime() );
		while( true ) {
			boolean eventOccured = es.recv( stepper.getNextInternalUpdateTime(), buf );
			stepper = stepper.update(buf.time, eventOccured ? buf.data : null);
		}
	}
}
