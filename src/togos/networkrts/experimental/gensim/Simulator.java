package togos.networkrts.experimental.gensim;

import togos.networkrts.experimental.netsim2.EventHandler;

public class Simulator
{
	public TimedEventQueue<Timestamped> teq = new TimedEventQueue<Timestamped>();
	public EventHandler eventHandler;
	/** Simulation time at which the currently evaluating event supposedly occurs */
	protected long procTime; 
	
	public long getSimulationTime() {
		return procTime;
	}
		
	public void run() throws Exception {
		while( !Thread.interrupted() ) {
			Timestamped evt;
			try {
				evt = teq.take();
				if( evt.getTimestamp() > procTime ) {
					procTime = evt.getTimestamp();
				}
			} catch( InterruptedException e ) {
				Thread.currentThread().interrupt();
				return;
			}
			eventHandler.eventOccured( evt );
		}
	}
}
