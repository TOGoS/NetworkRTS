package togos.networkrts.experimental.gensim5;

public class MainLoop
{
	public static <EventClass> void run( RealTimeEventSource<EventClass> es, Stepper<EventClass> stepper ) throws Exception {
		while( true ) {
			EventClass evt = es.recv( stepper.getNextInternalUpdateTime() );
			stepper.setCurrentTime(es.getCurrentTime());
			if( evt != null ) stepper.handleEvent(evt);
		}
	}
}
