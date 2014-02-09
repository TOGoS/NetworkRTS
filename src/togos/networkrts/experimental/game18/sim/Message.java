package togos.networkrts.experimental.game18.sim;

public final class Message
{
	public static final Message NONE = new Message(IDUtil.MAX_ID, IDUtil.MIN_ID, MessageType.NOOP, "no message");
	
	enum MessageType {
		NOOP,
		DELETE,
		INFORMATIONAL, // Like log messages
		ADD_DYNAMIC_THING,
		// etc
	}
	
	public final long minId;
	public final long maxId;
	public final MessageType type;
	public final Object payload;
	
	public Message( long minId, long maxId, MessageType type, Object payload ) {
		this.minId = minId; this.maxId = maxId;
		this.type = type; this.payload = payload;
	}
	
	public Message( long targetId, MessageType type, Object payload ) {
		this( targetId, targetId, type, payload );
	}
}
