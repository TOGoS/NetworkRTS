package togos.networkrts.experimental.gensim;

import togos.networkrts.util.Timer;

public class EventLoop
{
	public interface TimeTranslator {
		long simToReal( long simTime );
		long realToSim( long realTime );
	}
	public static final TimeTranslator IDENTITY_TIME_TRANSLATOR = new TimeTranslator() {
		@Override public long simToReal(long simTime) { return simTime; }
		@Override public long realToSim(long realTime) { return realTime; }
	};
	
	private EventLoop() { }
	
	// Style A
	public static <EventClass> void run( TimedEventQueue<EventClass> teq, EventUpdatable<EventClass> eventHandler ) throws Exception {
		long curTime = Long.MIN_VALUE;
		while( !Thread.interrupted() ) {
			Timer<EventClass> evt = teq.take();
			// Real-time events may come in marked for the past;
			// in that case, do not rewind time
			if( evt.time > curTime ) curTime = evt.time; 
			eventHandler = eventHandler.update( curTime, evt.payload );
		}
	}
	
	// Style B
	// (terminates when eventSource has no more events and next internal update time = infinity)
	public static <EventClass> void run( RealTimeEventSource<EventClass> eventSource, AutoEventUpdatable<EventClass> stepper, TimeTranslator timeTranslator ) throws Exception {
		EventBuffer<EventClass> buf = new EventBuffer<EventClass>( eventSource.getCurrentTime() );
		while( eventSource.hasMoreEvents() || stepper.getNextAutoUpdateTime() != AutoEventUpdatable.TIME_INFINITY ) {
			boolean eventOccured = eventSource.recv( timeTranslator.simToReal(stepper.getNextAutoUpdateTime()), buf );
			stepper = stepper.update(timeTranslator.realToSim(buf.time), eventOccured ? buf.payload : null);
		}
	}
	
	public static <EventClass> void run( RealTimeEventSource<EventClass> eventSource, AutoEventUpdatable<EventClass> stepper ) throws Exception {
		run( eventSource, stepper, IDENTITY_TIME_TRANSLATOR );
	}
}
