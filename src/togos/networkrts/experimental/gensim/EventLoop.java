package togos.networkrts.experimental.gensim;

import togos.networkrts.util.Timed;

public class EventLoop
{
	private EventLoop() { }
	
	// Style A
	public static <EventClass> void run( TimedEventQueue<EventClass> teq, TimedEventHandler<EventClass> eventHandler ) throws Exception {
		long prevTime = Long.MIN_VALUE;
		while( !Thread.interrupted() ) {
			Timed<EventClass> evt = teq.take();
			// Real-time events may come in marked for the past;
			// in that case, do not rewind time:
			if( evt.time > prevTime ) eventHandler.setCurrentTime( evt.time );
			eventHandler.handleEvent( evt.payload );
			prevTime = evt.time;
		}
	}
	
	// Style B
	public static <EventClass> void run( RealTimeEventSource<EventClass> es, Stepper<EventClass> stepper ) throws Exception {
		while( true ) {
			EventClass evt = es.recv( stepper.getNextInternalUpdateTime() );
			stepper.setCurrentTime(es.getCurrentTime());
			if( evt != null ) stepper.handleEvent(evt);
		}
	}
}
