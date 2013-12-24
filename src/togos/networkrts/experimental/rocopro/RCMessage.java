package togos.networkrts.experimental.rocopro;

import java.io.Serializable;

public class RCMessage implements Serializable
{
	private static final long serialVersionUID = 1L;

	enum MessageType {
		OPEN_SUBSCRIPTION,
		CLOSE_SUBSCRIPTION, 
		GET,
		PUT,
		POST,
		INFO,
		ACK
	};
	
	final int channelId;
	final int order;
	/** True if the sender would like an ACK in response */
	final MessageType messageType;
	final String resourceName;
	final Object payload;
	final boolean requestingAck;
	
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
