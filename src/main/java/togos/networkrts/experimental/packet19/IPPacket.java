package togos.networkrts.experimental.packet19;

public interface IPPacket extends DataPacket
{
	public static final PacketPayloadCodec<IPPacket> CODEC = new DataPacketPayloadCodec<IPPacket>() {
		@Override public IPPacket decode(byte[] data, int offset, int length) throws MalformedDataException {
			// TODO: Check if it's v6 or v4!
			return new IP6Packet(data, offset, length);
		}
	};
	
	public byte getIpVersion() throws MalformedDataException;
	public IPAddress getSourceAddress() throws MalformedDataException;
	public IPAddress getDestinationAddress() throws MalformedDataException;
	public DataPacket getPayload() throws MalformedDataException;
}
