package togos.networkrts.simulation.event;

/**
 * Removes an object from the simulation
 */
public class RemoveObject extends SimulationEvent
{
	public String removedObjectId;
	
	public RemoveObject( long ts, String removedObjectId ) {
		super(ts);
		this.removedObjectId = removedObjectId;
	}
}
