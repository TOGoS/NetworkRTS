package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.shape.RectIntersector;

public class Message
{
	public static final Message[] EMPTY_LIST = new Message[0];
	
	enum MessageType {
		WALK_ATTEMPTED,
		GETTING_PUSHED,
		INCOMING_PACKET
	}
	
	public final long minId, maxId;
	public final RectIntersector targetShape;
	public final MessageType type;
	public final Object payload;
	
	public Message( long minId, long maxId, RectIntersector targetShape, MessageType type, Object payload ) {
		this.minId = minId; this.maxId = maxId;
		this.targetShape = targetShape;
		this.type = type;
		this.payload = payload;
	}
}
