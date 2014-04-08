package togos.networkrts.experimental.packet19;

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
}
