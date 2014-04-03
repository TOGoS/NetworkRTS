package togos.networkrts.experimental.packet19;

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
	
	@Override public byte getIpVersion() { return 6; }
	
	public byte getNextHeader() {
		ensureObjectPopulated();
		return nextHeader;
	}
	public IP6Address getSourceAddress() {
		ensureObjectPopulated();
		return sourceAddress;
	}
	public IP6Address getDestinationAddress() {
		ensureObjectPopulated();
		return destinationAddress;
	}
	@Override public DataPacket getPayload() {
		ensureObjectPopulated();
		return payload;
	}
}
