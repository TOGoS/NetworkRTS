package togos.networkrts.experimental.sim1.simulation;

import java.util.Collection;

import togos.networkrts.experimental.sim1.world.VisibleWorldState;

public interface Part
{
	/**
	 * Called when the part receives damage.
	 * Should return true if the part is destroyed.
	 * Compound parts should remove component parts when they are destroyed due to passed-on
	 * (or internally generated) damage.
	 * If a root part is destroyed, an object will be automatically removed
	 * by the simulator. 
	 */
	public boolean damaged( String hostObjectId, long ts, int amount, Collection eventQueue );
	public void packetReceived( String hostObjectId,long ts, InfoPacket packet, Collection eventQueue );
	public void worldViewed( String hostObjectId, long ts, VisibleWorldState vws, Collection eventQueue );
	public void hostCreated( String hostObjectId, long ts, Collection eventQueue );
	public int getMass();
}
