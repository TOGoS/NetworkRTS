package togos.networkrts.experimental.packet19;

import togos.networkrts.util.ByteUtil;

public class EthernetFrame extends ContainerDataPacket<DataPacket>
{
	public static final DataPacketPayloadCodec<EthernetFrame> CODEC = new DataPacketPayloadCodec<EthernetFrame>() {
		@Override public EthernetFrame decode(byte[] data, int offset, int length) throws MalformedDataException {
			return new EthernetFrame(data, offset, length);
		}
	};
	
	/*
	 * Based on experience and backed up by discussios like
	 * http://www.tinc-vpn.org/pipermail/tinc/2013-January/003163.html
	 * it seems that ethernet frames from TAP devices don't
	 * include a frame check sequence (FCS) CRC.
	 * 
	 * Which may help explain why there's no good documentation about how to
	 * calculate the FCS.  Unless you're bit-banging the ethernet frame down
	 * the wire, the hardware takes care of it.
	 * 
	 * The structure we receive from the TAP interface
	 * (Wireshark calls it an "Ethernet II" frame) is:
	 * 
	 *  6 bytes - destination MAC address
	 *  6 bytes - source MAC address
	 *  2 bytes - ethertype
	 *  
	 * I expect that 802.1Q tag would be encoded properly, but I haven't tested.
	 * 
	 * The payload length is not encoded;
	 * the payload simply takes up the rest of the frame.
	 * 
	 * There is no CRC/FCS.
	 * 
	 * This also jives with the assumptions of my earlier ethernet frame parsing
	 * attempts, e.g. togos.networkrts.experimental.tcp1.EthernetFrameHandler.
	 * 
	 * If there are extra bytes after a payload (based on the payload's internal
	 * size, e.g. an IP packet's 'total length'), wireshark will treat those
	 * as an FCS.
	 */
	
	protected long src, dest;
	/** 802.1Q TCI field. Leave 0 for none. */
	protected short tag;
	protected short etherType;
	
	protected int payloadOffset, payloadSize;
	
	public EthernetFrame( byte[] data, int offset, int length ) {
		super( data, offset, length );
	}
	
	public EthernetFrame( long dest, long src, short tag, short etherType, WackPacket payload ) {
		assert tag == 0 || (tag & 0x81000000) == 0x81000000; 
		
		this.objectPopulated = true;
		this.payloadPopulated = true;
		this.dest = dest;
		this.src = src;
		this.tag = tag;
		this.etherType = etherType;
		this.payload = payload;
	}
	
	protected void ensureMinDataSize( int minSize, String forWhat ) {
		if( dataSize < minSize ) {
			throw new MalformedDataException("EthernetFrame data is too short "+(forWhat.length() == 0 ? "" : forWhat+" ")+"("+dataSize+" bytes; must be at least "+minSize+")");
		}
	}
	
	@Override protected void populateObject() {
		ensureMinDataSize(18, "");
		
		dest = ByteUtil.decodeUInt48(data, dataOffset);
		src  = ByteUtil.decodeUInt48(data, dataOffset+6);
		int code = ByteUtil.decodeUInt16(data, dataOffset+12) & 0xFFFF;
		if( code == 0x8100 ) {
			ensureMinDataSize(18, "to contain 802.1Q tag");
			// 802.1Q tag!
			tag = (short)ByteUtil.decodeUInt16(data, dataOffset+14);
			payloadOffset = dataOffset+18;
			code = ByteUtil.decodeUInt16(data, dataOffset+16);
		} else {
			payloadOffset = dataOffset+14;
			tag = 0;
			etherType = (short)code;
		}
		if( code <= 1500 ) {
			etherType = 0;
			payloadSize = code;
		} else {
			etherType = (short)code;
			// Payload is remainder of frame.
			payloadSize = dataSize+dataOffset - payloadOffset;
		}
		
		if( payloadOffset+payloadSize > dataOffset+dataSize  ) {
			throw new MalformedDataException(
				"EthernetFrame data is too short ("+dataSize+")"+
				" to contain supposed payload of "+payloadSize+
				" bytes at offset "+(payloadOffset-dataOffset));
		}
	}
	
	public long getSourceAddress() {
		ensureObjectPopulated();
		return src;
	}
	
	public long getDestinationAddress() {
		ensureObjectPopulated();
		return src;
	}
	
	public short getTag() {
		ensureObjectPopulated();
		return tag;
	}
	
	public short getEtherType() {
		ensureObjectPopulated();
		return etherType;
	}
	
	@Override public void populatePayload() {
		ensureObjectPopulated();
		switch( etherType ) {
		case EtherTypes.IP6: case EtherTypes.IP4:
			assert payloadOffset >= dataOffset + 14;
			payload = IPPacket.CODEC.decode(data, payloadOffset, payloadSize);
			break;
		default:
			System.err.println(String.format(
				"Frame contains unsupported EtherType 0x%04x; payload will be wack",
				(etherType&0xFFFF)));
			payload = new WackPacket(data, payloadOffset, payloadSize);
		}
	}
	
	public static final String format( long addy ) {
		return String.format("%02x:%02x:%02x:%02x:%02x:%02x",
			(addy >> 40) & 0xFF,
			(addy >> 32) & 0xFF,
			(addy >> 24) & 0xFF,
			(addy >> 16) & 0xFF,
			(addy >>  8) & 0xFF,
			(addy >>  0) & 0xFF
		);
	}
	
	public String toString() {
		if( objectPopulated ) {
			return "EthernetFrame to "+format(dest)+" from "+format(src)+" payload "+payload;
		} else {
			return "EthernetFrame (unparsed) length "+dataSize;
		}
	}

	public static int encodeHeader(
		long destAddress, long sourceAddress, short etherType,
		byte[] dest, int offset
	) {
		ByteUtil.encodeInt48(destAddress  , dest, offset+ 0);
		ByteUtil.encodeInt48(sourceAddress, dest, offset+ 6);
		ByteUtil.encodeInt16(etherType    , dest, offset+12);
		return 14;
	}
}
