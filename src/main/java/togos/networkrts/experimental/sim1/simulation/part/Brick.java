package togos.networkrts.experimental.sim1.simulation.part;

import java.util.Collection;

import togos.networkrts.experimental.sim1.simulation.InfoPacket;
import togos.networkrts.experimental.sim1.simulation.Part;
import togos.networkrts.experimental.sim1.world.VisibleWorldState;

public class Brick implements Part
{
	protected int mass;
	
	public Brick( int mass ) {
		this.mass = mass;
	}
	
	public boolean damaged( String hostObjectId, long ts, int amount, Collection eventQueue ) {
		mass -= amount;
		return mass < 0;
	}
	public void packetReceived( String hostObjectId, long ts, InfoPacket packet, Collection eventQueue ) {}
	public void worldViewed( String hostObjectId, long ts, VisibleWorldState vws, Collection eventQueue ) {}
	public void hostCreated( String hostObjectId, long ts, Collection eventQueue ) {}

	public int getMass() {
		return mass;
	}
}
