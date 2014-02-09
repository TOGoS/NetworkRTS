package togos.networkrts.experimental.game19.world;

public class Message
{
	public static final Message[] EMPTY_LIST = new Message[0];
	
	enum MessageType {
		GETTING_PUSHED,
		INCOMING_PACKET
	}
	
	public final long minId, maxId;
	public final int minX, minY, maxX, maxY;
	public final MessageType type;
	public final Object payload;
	
	public Message( long minId, long maxId, int minX, int minY, int maxX, int maxY, MessageType type, Object payload ) {
		this.minId = minId; this.maxId = maxId;
		this.minX = minX; this.maxX = maxX; this.minY = minY; this.maxY = maxY;
		this.type = type;
		this.payload = payload;
	}
}
