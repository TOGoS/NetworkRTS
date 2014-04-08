package togos.networkrts.experimental.packet19;

import togos.networkrts.util.ByteUtil;

public class EthernetFrame extends BaseDataPacket
{
	public static final DataPacketPayloadCodec<EthernetFrame> CODEC = new DataPacketPayloadCodec<EthernetFrame>() {
		@Override public EthernetFrame decode(byte[] data, int offset, int length) throws MalformedDataException {
			return new EthernetFrame(data, offset, length);
		}
	};
	
	protected long src, dest;
	protected WackPacket payload;
	protected int tag; // Leave 0 for no tag
	protected short etherType;
	protected int crc;
	
	public EthernetFrame( byte[] data, int offset, int length ) {
		super( data, offset, length );
	}
	
	public EthernetFrame( long dest, long src, int tag, short etherType, WackPacket payload ) {
		assert tag == 0 || (tag & 0x81000000) == 0x81000000; 
		
		this.objectPopulated = true;
		this.dest = dest;
		this.src = src;
		this.tag = tag;
		this.etherType = etherType;
		this.payload = payload;
	}
	
	@Override protected void populateObject() {
		if( dataSize < 14 ) throw new MalformedDataException("EthernetFrame is too short ("+dataSize+" bytes)");
		
		dest = ByteUtil.decodeInt48(data, dataOffset);
		src  = ByteUtil.decodeInt48(data, dataOffset+6);
		int code = ByteUtil.decodeInt16(data, dataOffset+12) & 0xFFFF;
		// TODO: Look at code to determine stuff
		int payloadOffset, payloadLength;
		if( code <= 1500 ) {
			payloadLength = code;
			payloadOffset = dataOffset+14;
		} else if( code == 0x86DD ) {
			// It's IPv6!
			// TODO: support other ethertypes
			etherType = (short)code;
			payloadOffset = dataOffset+14;
			payloadLength = dataOffset+dataSize - payloadOffset - 4;
		} else {
			throw new MalformedDataException(String.format("EthernetFrame is too fancy; code = 0x%04x", code));
		}
		
		if( dataOffset+dataSize < payloadOffset+payloadLength+4 ) {
			throw new MalformedDataException(
				"EthernetFrame data is too short ("+dataSize+")"+
				" to contain supposed payload of "+payloadLength+
				" bytes at offset "+(payloadOffset-dataOffset));
		}
		
		payload = new WackPacket(data, payloadOffset, payloadLength);
		// TODO: Check the CRC at this point?
		crc = ByteUtil.decodeInt32(data, payloadOffset+payloadLength);
	}
	
	public long getSourceAddress() {
		ensureObjectPopulated();
		return src;
	}
	
	public long getDestinationAddress() {
		ensureObjectPopulated();
		return src;
	}
	
	public WackPacket getPayload() {
		ensureObjectPopulated();
		return payload;
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
			return "EthernetFrame to "+format(dest)+" from "+format(src)+" payload "+payload+" crc "+crc;
		} else {
			return "EthernetFrame (unparsed) length "+dataSize;
		}
	}
}
