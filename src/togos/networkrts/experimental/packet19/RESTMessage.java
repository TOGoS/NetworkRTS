package togos.networkrts.experimental.packet19;

public interface RESTMessage<P> extends RESTRequest<P>, RESTResponse<P>
{
	public enum RESTMessageType {
		REQUEST,
		RESPONSE
	}
	
	public RESTMessageType getMessageType();
	/**
	 * For CoAP compatibility, responses tokens must have
	 * the same representation (value and number of bytes)
	 * as request token.  Therefore we need to track
	 * the number of bytes.
	 */
	public byte getTokenSignificantBytes();
	public long getToken();
}
