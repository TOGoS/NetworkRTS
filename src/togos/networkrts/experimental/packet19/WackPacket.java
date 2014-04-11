package togos.networkrts.experimental.packet19;

/**
 * A blob that may encode ~something~,
 * but in which the type and encoding/decoding procedure of that thing is unknown
 * to the packet itself.
 * 
 * Callers provide a codec when asking the wack packet for its payload,
 * and the wack packet will cache the most recently asked for interpretation
 * based on the class asked for.
 */
public class WackPacket extends BaseDataPacket
{
	protected Object payload;
	protected Class<?> payloadClass;
	protected PacketPayloadCodec<?> payloadCodec;
	
	public WackPacket( byte[] data, int offset, int length ) {
		super(data, offset, length);
	}
	
	public <T> WackPacket( T payload, Class<T> payloadClass, PacketPayloadCodec<T> payloadCodec ) {
		setPayload(payload, payloadClass, payloadCodec);
	}
	
	protected synchronized <T> void setPayload( T payload, Class<T> payloadType, PacketPayloadCodec<T> payloadCodec ) {
		this.payload = payload;
		this.payloadClass  = payloadType;
		this.payloadCodec = payloadCodec;
	}
	
	protected synchronized <T> T maybeGetPayload( Class<T> asClass ) {
		return asClass == payloadClass ? asClass.cast(payload) : null;
	}
	
	public <T> T getPayload( Class<T> asClass, PacketPayloadCodec<T> codec )
		throws MalformedDataException
	{
		T thang = maybeGetPayload(asClass);
		if( thang != null ) return thang;
		
		ensureDataPopulated();
		thang = codec.decode(data, dataOffset, dataSize);
		setPayload( thang, asClass, codec );
		return thang;
	}
	
	@Override public String toString() {
		if( payloadClass == null ) {
			return "WackPacket payload not populated";
		} else {
			return "WackPacket payload "+
				(payload instanceof DataPacket ? ((DataPacket)payload).toAtomicString() : payload.toString()); 
		}
	}
}
