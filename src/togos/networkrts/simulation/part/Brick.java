package togos.networkrts.simulation.part;

import java.util.Collection;

import togos.networkrts.simulation.InfoPacket;
import togos.networkrts.simulation.Part;
import togos.networkrts.world.VisibleWorldState;

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
