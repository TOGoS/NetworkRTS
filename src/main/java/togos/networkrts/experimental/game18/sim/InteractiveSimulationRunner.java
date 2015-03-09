package togos.networkrts.experimental.game18.sim;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class InteractiveSimulationRunner
{
	public final BlockingQueue<Message> incomingMessageQueue;
	public final Simulation sim;
	public final float realtimeMillisecondsPerSimulationTick;
	
	public InteractiveSimulationRunner( BlockingQueue<Message> incomingMessageQueue, Simulation sim, float realtimeMsPerGameTick ) {
		this.incomingMessageQueue = incomingMessageQueue;
		this.sim = sim;
		this.realtimeMillisecondsPerSimulationTick = realtimeMsPerGameTick;
	}
	
	public void run() {
		long cRT = 0, nRT;
		long cST = 0, nST;
		while( !Thread.interrupted() ) {
			nST = sim.getNextAutoUpdateTime();
			if( cST == 0 ) {
				cST = nST; 
				cRT = nRT = System.currentTimeMillis();
			} else {
				cRT = System.currentTimeMillis();
				nRT = nST == Long.MAX_VALUE ? Long.MAX_VALUE : cRT + (long)(realtimeMillisecondsPerSimulationTick*(nST-cST));
			}
			Message m;
			try {
				m = incomingMessageQueue.poll( nRT-cRT, TimeUnit.MILLISECONDS );
			} catch( InterruptedException e ) {
				Thread.currentThread().interrupt();
				return;
			}
			if( m != null ) {
				// Adjust 'next [real|simulation] time'
				nRT = System.currentTimeMillis();
				nST = cST + (long)((nRT-cRT)/realtimeMillisecondsPerSimulationTick);
				if( nST < cST ) nST = cST;
			} else {
				m = Message.NONE;
			}
			//System.err.println("Updating to "+nts);
			sim.update( nST, m );
			cST = nST;
		}
	}
}
