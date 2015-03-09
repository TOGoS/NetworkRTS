package togos.networkrts.experimental.inet.transport;

public interface PacketListener<Packet>
{
	public void packetReceived( Packet p );
}
