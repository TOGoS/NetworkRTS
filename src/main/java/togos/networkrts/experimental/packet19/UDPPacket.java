package togos.networkrts.experimental.packet19;


public class UDPPacket extends BaseDataPacket
{
	protected short sourcePort, destinationPort;
	protected short checksum;
	protected final IPPacket wrappingPacket;
	protected WackPacket payload;
	
	// Note: Creating or verifying the checksum requires
	// access to the wrapping packet, so that must be provided by either constructor.
	
	public UDPPacket( IPPacket wrappingPacket, byte[] buffer, int offset, int length ) {
		super(buffer, offset, length);
		this.wrappingPacket = wrappingPacket;
		// TODO: since checksum is part of the 'data', it should be populated here
	}
	
	public UDPPacket(
		IPPacket wrappingPacket, short sourcePort, short destinationPort,
		WackPacket payload
	) {
		this.objectPopulated = true;
		this.sourcePort = sourcePort;
		this.destinationPort = destinationPort;
		this.wrappingPacket = wrappingPacket;
		this.payload = payload;
	}
	
	public short getSourcePort() {
		ensureObjectPopulated();
		return sourcePort;
	}
	public short getDestinationPort() {
		ensureObjectPopulated();
		return destinationPort;
	}
	public short getChecksum() {
		ensureDataPopulated();
		return checksum;
	}
	public WackPacket getPayload() {
		ensureObjectPopulated();
		return payload;
	}
}
