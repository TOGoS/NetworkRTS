package togos.networkrts.experimental.packet19;

/**
 * Intermediate solution until I get the real CoAP message class working.
 */
public class FakeCoAPMessage implements RESTMessage
{
	protected final RESTMessageType type;
	protected final byte tokenLength;
	protected final long token;
	protected final String method;
	protected final String path;
	protected final int status;
	protected final WackPacket payload;
	
	private FakeCoAPMessage( RESTMessageType type, byte tokenLength, long token, String method, String path, int status, WackPacket payload ) {
		assert tokenLength >= 0;
		assert tokenLength <= 8;
		
		this.type = type;
		this.tokenLength = tokenLength;
		this.token = token;
		this.method = method;
		this.path = path;
		this.status = status;
		this.payload = payload;
	}
	
	public static FakeCoAPMessage request( byte tokenLength, long token, String method, String path, WackPacket payload ) {
		return new FakeCoAPMessage( RESTMessageType.REQUEST, tokenLength, token, method, path, 0, payload );
	}
	
	public static FakeCoAPMessage response( byte tokenLength, long token, int status, WackPacket payload ) {
		return new FakeCoAPMessage( RESTMessageType.RESPONSE, tokenLength, token, null, null, status, payload );
	}
	
	@Override public RESTMessageType getRestMessageType() { return type; }
	@Override public byte getTokenSignificantBytes() { return tokenLength; }
	@Override public long getToken() { return token; }
	@Override public String getMethod() { return method; }
	@Override public String getPath() { return path; }
	@Override public int getStatus() { return status; }
	@Override public WackPacket getPayload() { return payload; }
}
