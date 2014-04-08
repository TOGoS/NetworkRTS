package togos.networkrts.experimental.packet19;

import togos.blob.ByteChunk;

public interface IPPacket extends DataPacket
{
	public static final PacketPayloadCodec<IPPacket> CODEC = new DataPacketPayloadCodec<IPPacket>() {
		@Override public IPPacket decode(byte[] data, int offset, int length) throws MalformedDataException {
			// TODO: Check if it's v6 or v4!
			return new IP6Packet(data, offset, length);
		}
	};
	
	public byte getIpVersion() throws MalformedDataException;
	public ByteChunk getSourceAddress() throws MalformedDataException;
	public ByteChunk getDestinationAddress() throws MalformedDataException;
	public DataPacket getPayload() throws MalformedDataException;
}
