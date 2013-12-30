package togos.networkrts.experimental.inet.transport;

public interface NetworkInterface<Packet>
{
	public void sendPacket( Packet p );
	public void addIncomingPacketListener( PacketListener<Packet> pl );
}
