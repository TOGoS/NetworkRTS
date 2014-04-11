package togos.networkrts.experimental.packet19;

public class ContainerDataPacket<Payload> extends BaseDataPacket
{
	protected boolean payloadPopulated;
	protected Payload payload;
	
	protected ContainerDataPacket() { }
	
	protected ContainerDataPacket( byte[] data, int offset, int size ) {
		super( data, offset, size );
	}

	protected void populatePayload() {
		throw new UnsupportedOperationException();
	}
	
	protected synchronized void ensurePayloadPopulated() {
		if( !payloadPopulated ) populatePayload();
		payloadPopulated = true;
	}
	
	public Payload getPayload() {
		ensurePayloadPopulated();
		return payload;
	}
}
