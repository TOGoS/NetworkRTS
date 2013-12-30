package togos.networkrts.experimental.rocopro;

import java.io.Serializable;

public class RCMessage implements Serializable
{
	private static final long serialVersionUID = 1L;

	public enum MessageType {
		OPEN_SUBSCRIPTION,
		CLOSE_SUBSCRIPTION, 
		GET,
		PUT,
		POST,
		INFO,
		ACK
	};
	
	public final int channelId;
	public final int order;
	/** True if the sender would like an ACK in response */
	public final MessageType messageType;
	public final String resourceName;
	public final Object payload;
	public final boolean requestingAck;
	
	public RCMessage(
		int channelId, int order, MessageType messageType, String resourceName,
		Object payload, boolean requestingAck
	) {
		this.channelId = channelId;
		this.order = order;
		this.messageType = messageType;
		this.resourceName = resourceName;
		this.payload = payload;
		this.requestingAck = requestingAck;
	}
}
