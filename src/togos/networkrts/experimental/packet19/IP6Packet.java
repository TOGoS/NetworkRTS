package togos.networkrts.experimental.packet19;

import togos.blob.ByteChunk;

public class IP6Packet extends BaseDataPacket implements IPPacket
{
	// TODO: Add other fields
	
	protected byte nextHeader;
	protected ByteChunk sourceAddress;
	protected ByteChunk destinationAddress;
	protected DataPacket payload;
	
	public IP6Packet( byte[] data, int offset, int length ) {
		super(data, offset, length);
	}
	private IP6Packet( byte nextHeader, ByteChunk sourceAddress, ByteChunk destAddress, DataPacket payload ) {
		this.objectPopulated = true;
		this.nextHeader = nextHeader;
		this.sourceAddress = sourceAddress;
		this.destinationAddress = destAddress;
		this.payload = payload;
	}
	
	public static IP6Packet create( byte nextHeader, ByteChunk sourceAddress, ByteChunk destAddress, DataPacket payload ) {
		return new IP6Packet( nextHeader, sourceAddress, destAddress, payload );
	}
	
	@Override public byte getIpVersion() { return 6; }
	
	public byte getNextHeader() {
		ensureObjectPopulated();
		return nextHeader;
	}
	public ByteChunk getSourceAddress() {
		ensureObjectPopulated();
		return sourceAddress;
	}
	protected ByteChunk getDestinationAddress() {
		ensureObjectPopulated();
		return destinationAddress;
	}
}
