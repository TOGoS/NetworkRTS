package togos.networkrts.experimental.packet19;

/**
 * Intermediate solution until I get the real CoAP message class working.
 */
public class FakeCoAPMessage<P> implements RESTMessage<P>
{
	protected final RESTMessageType type;
	protected final byte tokenLength;
	protected final long token;
	protected final String method;
	protected final String path;
	protected final int status;
	protected final P payload;
	
	private FakeCoAPMessage( RESTMessageType type, byte tokenLength, long token, String method, String path, int status, P payload ) {
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
	
	public static <P> FakeCoAPMessage<P> request( byte tokenLength, long token, String method, String path, P payload ) {
		return new FakeCoAPMessage<P>( RESTMessageType.REQUEST, tokenLength, token, method, path, 0, payload );
	}
	
	public static <P> FakeCoAPMessage<P> response( byte tokenLength, long token, int status, P payload ) {
		return new FakeCoAPMessage<P>( RESTMessageType.RESPONSE, tokenLength, token, null, null, status, payload );
	}
	
	@Override public RESTMessageType getMessageType() { return type; }
	@Override public byte getTokenSignificantBytes() { return tokenLength; }
	@Override public long getToken() { return token; }
	@Override public String getMethod() { return method; }
	@Override public String getPath() { return path; }
	@Override public int getStatus() { return status; }
	@Override public P getPayload() { return payload; }
}
