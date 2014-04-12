package togos.networkrts.experimental.packet19;

import togos.blob.ByteChunk;
import togos.blob.SimpleByteChunk;
import togos.networkrts.inet.InternetChecksum;
import togos.networkrts.util.ByteUtil;

public class ICMP6Packet extends BaseDataPacket
{
	public static final byte PING6 = (byte)128;
	public static final byte PONG6 = (byte)129;
	public static final byte NEIGHBOR_SOLICITATION = (byte)135;
	public static final byte NEIGHBOR_ADVERTISEMENT = (byte)136;
	
	public static final int NEIGHBOR_ADVERTISEMENT_SIZE = 32;
	
	public final IP6Packet ip6Packet;
	protected ByteChunk payload;
	
	public ICMP6Packet( IP6Packet wrappingPacket, byte[] data, int offset, int size ) {
		super(data, offset, size);
		this.ip6Packet = wrappingPacket;
	}
	
	public ICMP6Packet( IP6Packet wrappingPacket, byte type, byte code, ByteChunk payload ) {
		this.ip6Packet = wrappingPacket;
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
	
	/**
	 * Assuming this is a neighbor solicitatio message (type = 135)
	 * return true iff the target address contained in the ICMP payload
	 * is equal to the provided address.
	 */
	public boolean checkNeighborSolicitationTargetAddress( IP6Address a ) {
		if( dataSize < 24 ) return false;
		return ByteUtil.equals( a, data, dataOffset+8 );
	}
	
	protected static short calcCrc(
		IP6Address sourceAddy, IP6Address destAddy,
		byte[] icmp6Data, int icmp6Offset, int icmp6Length
	) {
		byte[] crcBuf = new byte[icmp6Length+40];
		ByteUtil.copy(sourceAddy, crcBuf,  0);
		ByteUtil.copy(  destAddy, crcBuf, 16);
		ByteUtil.encodeInt32(icmp6Length, crcBuf, 32);
		//crcBuf[36] = IPProtocols.ICMP6;
		crcBuf[39] = IPProtocols.ICMP6;
		ByteUtil.copy(icmp6Data, icmp6Offset, crcBuf, 40, icmp6Length);
		return (short)InternetChecksum.checksum(crcBuf);
	}
	
	public static int encodeNeighborAdvertisement(
		IP6Address sourceAddress, IP6Address destAddress, IP6Address advertAddress,
		boolean router, boolean wasSolicited, boolean override,
		long advertLinkAddress,
		byte[] data, int offset
	) {
		final int icmp6Length = NEIGHBOR_ADVERTISEMENT_SIZE;
		assert data.length >= offset + icmp6Length;
		
		data[offset  ] = NEIGHBOR_ADVERTISEMENT;
		data[offset+1] = 0;
		data[offset+2] = 0; // Zeroed-out checksum
		data[offset+3] = 0; // Zeroed-out checksum
		int flags = 0;
		if( router       ) flags |= 0x80000000;
		if( wasSolicited ) flags |= 0x40000000;
		if( override     ) flags |= 0x20000000;
		ByteUtil.encodeInt32(flags, data, offset+4);
		ByteUtil.copy(advertAddress, data, offset+8);
		
		data[offset+24] = 2; // Option type = target link-layer address
		data[offset+25] = 1; // Length = 1 * 8 bytes
		ByteUtil.encodeInt48(advertLinkAddress, data, offset+26);
		
		short crc = calcCrc(sourceAddress, destAddress, data, offset, icmp6Length);
		ByteUtil.encodeInt16(crc, data, offset+2);
		return offset + icmp6Length;
	}

	public static int encodePong(
		IP6Address sourceAddress, IP6Address destAddress, ICMP6Packet ping,
		byte[] data, int offset
	) {
		int size = ping.getSize();
		data[offset  ] = PONG6;
		data[offset+1] = 0;
		data[offset+2] = 0; // Zeroed-out checksum
		data[offset+3] = 0; // Zeroed-out checksum
		ByteUtil.copy(ping.getBuffer(), ping.getOffset()+4, data, offset+4, ping.getSize()-4);
		
		short crc = calcCrc(sourceAddress, destAddress, data, offset, size);
		ByteUtil.encodeInt16(crc, data, offset+2);
		return offset+size;
	}
}
