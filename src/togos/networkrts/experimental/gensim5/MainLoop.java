package togos.networkrts.experimental.gensim5;

public class MainLoop<EventClass>
{
	public void run( RealTimeEventSource<EventClass> es, Simulation<EventClass> world ) {
		while( true ) {
			EventClass evt = es.recv( world.getNextInternalUpdateTime() );
			world.setCurrentTime(es.getCurrentTime());
			if( evt != null ) world.handleEvent(evt);
		}
	}
}
