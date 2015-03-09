package togos.networkrts.experimental.sim1.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;

import togos.networkrts.experimental.sim1.simulation.event.AddObject;
import togos.networkrts.experimental.sim1.simulation.event.DeliverPacket;
import togos.networkrts.experimental.sim1.simulation.event.RemoveObject;
import togos.networkrts.experimental.sim1.simulation.event.SimulationEvent;

public class Simulator
{
	protected PriorityQueue eventQueue = new PriorityQueue();
	protected HashMap simulationObjects = new HashMap();
	
	String baseId = Long.toHexString(System.currentTimeMillis()^new Random().nextLong()) + "-";
	long nextId = 0;
	
	protected String newObjectId() {
		return baseId + (nextId++);
	}
	
	public void addObject( long ts, SimulationObject o ) {
		String id = newObjectId();
		simulationObjects.put( id, o );
		System.err.println("Added "+id);
		o.rootPart.hostCreated(id, ts, eventQueue);
	}
	
	protected void handleEvent( SimulationEvent e ) {
		if( e instanceof AddObject ) {
			addObject( e.triggerTimestamp, ((AddObject)e).newObject );
		} else if( e instanceof RemoveObject ) {
			simulationObjects.remove( ((RemoveObject)e).removedObjectId );
			System.err.println("Removed "+((RemoveObject)e).removedObjectId);
		} else if( e instanceof DeliverPacket ) {
			DeliverPacket dp = (DeliverPacket)e;
			SimulationObject so = (SimulationObject)simulationObjects.get(dp.destObjectId);
			if( so == null ) return;
			so.rootPart.packetReceived(dp.destObjectId, dp.triggerTimestamp, dp.packet, eventQueue);
		}
	}
	
	public void runUntil( long endTimestamp ) {
		SimulationEvent se = (SimulationEvent)eventQueue.poll();
		while( se != null && se.triggerTimestamp < endTimestamp ) {
			// TODO: Collision and other physics calculations!
			
			handleEvent(se);
			se = (SimulationEvent)eventQueue.poll();
		}
		if( se != null ) {
			eventQueue.add(se);
		}
	}
	
	public Collection getAllObjects() {
		return new ArrayList(simulationObjects.values());
	}
}
