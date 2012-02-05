package togos.networkrts.simulation.event;

import togos.networkrts.simulation.InfoPacket;

public class DeliverPacket extends SimulationEvent
{
	public String destObjectId;
	public InfoPacket packet;
	
	public DeliverPacket( long ts, String destObjectId, InfoPacket packet ) {
		super(ts);
		this.destObjectId = destObjectId;
		this.packet = packet;
	}
}
