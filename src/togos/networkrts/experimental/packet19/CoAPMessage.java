package togos.networkrts.experimental.packet19;

public class CoAPMessage extends BaseDataPacket implements RESTMessage
{
	public static final PacketPayloadCodec<CoAPMessage> CODEC = new DataPacketPayloadCodec<CoAPMessage>() {
		@Override public CoAPMessage decode(byte[] data, int offset, int length) throws MalformedDataException {
			return new CoAPMessage(data, offset, length);
		}
	};
	
	// TODO: Implement serialization/deserialization
	
	enum CoAPMessageType {
		CONFIRMABLE,
		NON_CONFIRMABLE,
		ACKNOWLEDGEMENT,
		RESET
	};
	
	static class CoAPOption {
		final int optionNumber;
		final byte[] value;
		
		public CoAPOption( int optionNumber, byte[] value ) {
			this.optionNumber = optionNumber;
			this.value = value;
		}
	}

	private int header;
	private long token;
	private CoAPOption[] options;
	
	public CoAPMessage( byte[] data, int offset, int length ) {
		super(data, offset, length);
	}
	
	protected int getMessageHeader() throws MalformedDataException {
		ensureObjectPopulated();
		return header;
	}
	
	public CoAPMessageType getMessageType() {
		return CoAPMessageType.class.getEnumConstants()[(getMessageHeader() >> 28) & 0x3];
	}
	
	protected int getTokenLength() { return (getMessageHeader() >> 24) & 0xF; }
	protected int getCode() {        return (getMessageHeader() >> 16) & 0xFF; }
	protected int getMessageId() {   return  getMessageHeader() & 0xFFFF; }
	
	public long getToken() {
		ensureObjectPopulated();
		return token;
	}
	
	public CoAPOption[] getOptions() {
		ensureObjectPopulated();
		return options;
	}
	
	@Override public String getMethod() {
		throw new UnsupportedOperationException();
	}

	@Override public String getPath() {
		throw new UnsupportedOperationException();
	}

	@Override public WackPacket getPayload() {
		throw new UnsupportedOperationException();
	}

	@Override public int getStatus() {
		throw new UnsupportedOperationException();
	}

	@Override public RESTMessageType getRestMessageType() {
		throw new UnsupportedOperationException();
	}

	@Override public byte getTokenSignificantBytes() {
		throw new UnsupportedOperationException();
	}
}
