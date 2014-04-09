package togos.networkrts.experimental.packet19;

import java.util.zip.CRC32;

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
	/** 802.1Q TCI field. Leave 0 for none. */
	protected short tag;
	protected short etherType;
	protected int crc; // CRC actually present in the frame
	protected int calculatedCrc; // CRC calculated by us
	
	public EthernetFrame( byte[] data, int offset, int length ) {
		super( data, offset, length );
	}
	
	public EthernetFrame( long dest, long src, short tag, short etherType, WackPacket payload ) {
		assert tag == 0 || (tag & 0x81000000) == 0x81000000; 
		
		this.objectPopulated = true;
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
		
		dest = ByteUtil.decodeInt48(data, dataOffset);
		src  = ByteUtil.decodeInt48(data, dataOffset+6);
		int code = ByteUtil.decodeInt16(data, dataOffset+12) & 0xFFFF;
		final int payloadOffset;
		if( code == 0x8100 ) {
			ensureMinDataSize(18, "to contain 802.1Q tag");
			// 802.1Q tag!
			tag = (short)ByteUtil.decodeInt16(data, dataOffset+14);
			payloadOffset = dataOffset+18;
			code = ByteUtil.decodeInt16(data, dataOffset+16);
		} else {
			payloadOffset = dataOffset+14;
			tag = 0;
			etherType = (short)code;
		}
		final int payloadSize;
		if( code <= 1500 ) {
			etherType = 0;
			payloadSize = code;
		} else {
			etherType = (short)code;
			payloadSize = dataOffset+dataSize - payloadOffset - 4;
		}
		
		if( dataOffset+dataSize < payloadOffset+payloadSize+4 ) {
			throw new MalformedDataException(
				"EthernetFrame data is too short ("+dataSize+")"+
				" to contain supposed payload of "+payloadSize+
				" bytes at offset "+(payloadOffset-dataOffset));
		}
		
		payload = new WackPacket(data, payloadOffset, payloadSize);
		// TODO: Check the CRC at this point?
		crc = ByteUtil.decodeInt32(data, payloadOffset+payloadSize);
		
		System.err.println("Data size = "+dataSize+", payload size = "+payloadSize);
		calculatedCrc = calculateFcs(data, dataOffset, dataSize, false, true);
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
			return "EthernetFrame to "+format(dest)+" from "+format(src)+" payload "+payload+" CRC "+crc+" calculated CRC "+calculatedCrc;
		} else {
			return "EthernetFrame (unparsed) length "+dataSize;
		}
	}
	
	protected static int reverseBytes( int v0 ) {
		return
			((v0>>24)&0xFF) | ((v0>>8)&0xFF00) |
			((v0<<8)&0xFF0000) | ((v0<<24)&0xFF000000);
	}
	
	protected static int calculateFcs( byte[] data, int offset, int length, boolean addPadding, boolean reverseBytes ) {
		CRC32 c = new CRC32();
		c.update(data, offset, length);
		if( addPadding ) c.update(new byte[]{0,0,0,0});
		int v = (int)c.getValue();
		return reverseBytes ? reverseBytes( v ) : v;
	}
	
	/** My best guess as to the 'correct' way to calculate the FCS */
	protected static int calculateFcs( byte[] data, int offset, int length ) {
		return calculateFcs( data, offset, length, true, true );
	}
	
	protected static byte[] fromHex( String s ) {
		byte[] data = new byte[s.length()/2];
		for( int i=0; i<data.length; ++i ) {
			data[i] = (byte)Integer.parseInt(s.substring(i*2, i*2+2), 16);
		}
		return data;
	}
	
	static boolean[] TF = new boolean[]{true,false};
	
	public static void main( String[] args ) {
		// http://forums.xilinx.com/t5/Spartan-Family-FPGAs/CRC32-and-Packets-from-Ethenet/td-p/41305
		String hexPacket = "FFFFFFFFFFFF0020ED1C149B080600010800060400010020ED1C149BC804079C000000000000C8040783000000000000000000000000000000000000";
		byte[] packet = fromHex(hexPacket);
		for( boolean addPadding : TF ) {
			for( boolean reverseBytes : TF ) {
				int v = calculateFcs(packet, 0, packet.length, addPadding, reverseBytes);
				System.out.println(String.format("%08x%s%s", v, addPadding ? " (padded)" : "", reverseBytes ? " (output bytes reversed)" : ""));
			}
		}
	}
}
