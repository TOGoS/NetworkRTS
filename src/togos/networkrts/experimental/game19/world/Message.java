package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.shape.RectIntersector;
import togos.networkrts.util.BitAddressRange;

public class Message implements BitAddressRange
{
	public static final Message[] EMPTY_LIST = new Message[0];
	
	public enum MessageType {
		INCOMING_PACKET,
		NEIGHBOR_UPDATED
	}
	
	public final long minBitAddress, maxBitAddress;
	public final RectIntersector targetShape;
	public final MessageType type;
	public final Object payload;
	
	public Message( long minBa, long maxBa, RectIntersector targetShape, MessageType type, Object payload ) {
		this.minBitAddress = minBa; this.maxBitAddress = maxBa;
		this.targetShape = targetShape;
		this.type = type;
		this.payload = payload;
	}
	
	public Message( int targetId, RectIntersector targetShape, MessageType type, Object payload ) {
		this( BitAddresses.withMinFlags(targetId), BitAddresses.withMaxFlags(targetId), targetShape, type, payload );
	}

	@Override public long getMinBitAddress() { return minBitAddress; }
	@Override public long getMaxBitAddress() { return maxBitAddress; }
}
