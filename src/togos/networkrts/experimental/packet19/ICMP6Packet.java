package togos.networkrts.experimental.packet19;

import togos.blob.ByteChunk;
import togos.blob.SimpleByteChunk;
import togos.networkrts.util.ByteUtil;

public class ICMP6Packet extends BaseDataPacket
{
	public static final byte PING6 = (byte)128;
	public static final byte PONG6 = (byte)129;
	public static final byte NEIGHBOR_SOLICITATION = (byte)135;
	
	final IP6Packet wrappingPacket;
	protected ByteChunk payload;
	
	public ICMP6Packet( IP6Packet wrappingPacket, byte[] data, int offset, int size ) {
		super(data, offset, size);
		this.wrappingPacket = wrappingPacket;
	}
	
	public ICMP6Packet( IP6Packet wrappingPacket, byte type, byte code, ByteChunk payload ) {
		this.wrappingPacket = wrappingPacket;
		dataSize = payload.getSize() + 4;
		data = new byte[dataSize];
		ByteUtil.copy(payload, data, 4);
	}
	
	protected void validate() {
		if( dataSize < 4 ) throw new MalformedDataException("ICMP6 packet is too short ("+dataSize+" bytes)");
	}
	
	public byte getType() {
		validate();
		return data[dataOffset+0];
	}
	public byte getCode() {
		validate();
		return data[dataOffset+1];
	}
	public short getCrc() {
		validate();
		return ByteUtil.decodeInt16(data, dataOffset+2);
	}
	public short calculateCrc() {
		throw new UnsupportedOperationException();
	}
	public ByteChunk getPayload() {
		if( payload == null ) {
			validate();
			payload = dataSize == 4 ? SimpleByteChunk.EMPTY : new SimpleByteChunk(data, dataOffset+4, dataSize-4);
		}
		return payload;
	}
	
	public String toString() {
		try {
			return String.format("ICMP6Packet\n" +
				"type %d\n"+
				"code %d\n"+
				"crc 0x%04x\n"+
				"payload %s",
				getType()&0xFF,
				getCode()&0xFF,
				getCrc()&0xFFFF,
				dataSize - 4 + " bytes"
			);
		} catch( MalformedDataException e ) {
			return "ICMP6Packet (too short)";
		}
	}
}
