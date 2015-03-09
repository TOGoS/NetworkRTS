package togos.networkrts.experimental.sim1.simulation.event;

import togos.networkrts.experimental.sim1.simulation.SimulationObject;

/**
 * Adds an object to the simulation.
 * The root part's hostCreated method will be called.
 */
public class AddObject extends SimulationEvent
{
	public SimulationObject newObject;
	
	public AddObject( long ts, SimulationObject newObject ) {
		super(ts);
		this.newObject = newObject;
	}
}
