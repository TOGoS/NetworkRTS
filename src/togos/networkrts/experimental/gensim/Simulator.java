package togos.networkrts.experimental.gensim;

public class Simulator
{
	protected TimedEventQueue<Timestamped> teq = new TimedEventQueue<Timestamped>();
	protected WorldUpdater worldUpdater;
	/** Simulation time at which the currently evaluating event supposedly occurs */
	protected long procTime; 
	
	public long simulationTime() {
		return procTime;
	}
	
	// Arbitrary tick interval
	long tickInterval = 10;
	
	/** Return the simulation time of the next 'tick' after the given delay */
	public long tickAfterDelay( long delay ) {
		long destTime = procTime + delay;
		if( destTime % tickInterval > 0 ) {
			// Round up to next tick
			return destTime + tickInterval - (destTime % tickInterval);
		} else {
			return destTime;
		}
	}
	
	public void run() {
		while( !Thread.interrupted() ) {
			Timestamped evt;
			try {
				evt = teq.take( );
				if( evt.getTimestamp() > procTime ) {
					procTime = evt.getTimestamp();
				}
			} catch( InterruptedException e ) {
				Thread.currentThread().interrupt();
				return;
			}
			worldUpdater = worldUpdater.handleEvent( evt );
		}
	}
}
