package togos.networkrts.experimental.game18.sim;

import togos.networkrts.util.BitAddressRange;
import togos.networkrts.util.BitAddressUtil;

public final class Message implements BitAddressRange
{
	public static final Message NONE = new Message(BitAddressUtil.MAX_ADDRESS, BitAddressUtil.MIN_ADDRESS, MessageType.NOOP, "no message");
	
	enum MessageType {
		NOOP,
		DELETE,
		INFORMATIONAL, // Like log messages
		ADD_DYNAMIC_THING,
		// etc
	}
	
	public final long minBitAddress;
	public final long maxBitAddress;
	public final MessageType type;
	public final Object payload;
	
	public Message( long minBa, long maxBa, MessageType type, Object payload ) {
		this.minBitAddress = minBa; this.maxBitAddress = maxBa;
		this.type = type; this.payload = payload;
	}
	
	public Message( long targetId, MessageType type, Object payload ) {
		this( targetId, targetId, type, payload );
	}
	
	@Override public long getMinBitAddress() { return minBitAddress; }
	@Override public long getMaxBitAddress() { return maxBitAddress; }
}
