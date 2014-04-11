package togos.networkrts.experimental.packet19;

import togos.networkrts.util.ByteUtil;

public class IP6Packet extends BaseDataPacket implements IPPacket
{
	// TODO: Add other fields
	
	protected byte nextHeader;
	protected IP6Address sourceAddress;
	protected IP6Address destinationAddress;
	protected DataPacket payload;
	
	public IP6Packet( byte[] data, int offset, int length ) {
		super(data, offset, length);
	}
	private IP6Packet( byte nextHeader, IP6Address sourceAddress, IP6Address destAddress, DataPacket payload ) {
		this.objectPopulated = true;
		this.nextHeader = nextHeader;
		this.sourceAddress = sourceAddress;
		this.destinationAddress = destAddress;
		this.payload = payload;
	}
	
	public static IP6Packet create( byte nextHeader, IP6Address sourceAddress, IP6Address destAddress, DataPacket payload ) {
		return new IP6Packet( nextHeader, sourceAddress, destAddress, payload );
	}
	
	protected void ensureMinDataSize( int minSize, String forWhat ) {
		if( dataSize < minSize ) {
			throw new MalformedDataException("IP6Packet data is too short "+(forWhat.length() == 0 ? "" : forWhat+" ")+"("+dataSize+" bytes; must be at least "+minSize+")");
		}
	}
	
	@Override protected void populateObject() {
		ensureMinDataSize( 40, "fixed header" );
		int payloadSize = ByteUtil.decodeUInt16(data, dataOffset+4);
		// Payload length includes extension headers, so:
		ensureMinDataSize(40+payloadSize, "payload");
		nextHeader = data[dataOffset+6];
		sourceAddress = new IP6Address( data, dataOffset+8 );
		destinationAddress = new IP6Address( data, dataOffset+24 );
		switch( nextHeader ) {
		case IPProtocols.UDP:
			payload = new UDPPacket(this, data, dataOffset+40, payloadSize);
			break;
		default:
			System.err.println(String.format("Wacking payload for unsupported IP protocol 0x%02x", nextHeader));
			payload = new WackPacket(data, dataOffset+40, payloadSize);
		}
	}
	
	@Override public byte getIpVersion() { return 6; }
	
	public byte getNextHeader() throws MalformedDataException {
		ensureObjectPopulated();
		return nextHeader;
	}
	public IP6Address getSourceAddress() throws MalformedDataException {
		ensureObjectPopulated();
		return sourceAddress;
	}
	public IP6Address getDestinationAddress() throws MalformedDataException {
		ensureObjectPopulated();
		return destinationAddress;
	}
	@Override public DataPacket getPayload() throws MalformedDataException {
		ensureObjectPopulated();
		return payload;
	}
}
