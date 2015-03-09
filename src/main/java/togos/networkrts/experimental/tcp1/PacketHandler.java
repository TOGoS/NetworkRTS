package togos.networkrts.experimental.tcp1;

public interface PacketHandler
{
	public void handlePacket( byte[] data, int offset, int length );
}
