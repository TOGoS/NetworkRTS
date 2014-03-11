package togos.networkrts.experimental.game19.world;

import java.util.Collections;
import java.util.Iterator;

import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityRange;
import togos.networkrts.experimental.shape.RectIntersector;
import togos.networkrts.util.BitAddressRange;
import togos.networkrts.util.BitAddressUtil;

public class Message implements BitAddressRange, MessageSet
{
	public static final Message[] EMPTY_LIST = new Message[0];
	
	public enum MessageType {
		INCOMING_PACKET,
		NEIGHBOR_UPDATED
	}
	
	public final long minBitAddress, maxBitAddress;
	// TODO: Replace RectIntersector with something less sucky
	// so that isApplicableTo can take spatial range into account more easily
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
	
	public boolean isApplicableTo(
		double minX, double minY, double maxX,
		double maxY, long minBitAddress, long maxBitAddress
	) {
		// TODO: Check spatial range
		return BitAddressUtil.rangesIntersect(this, minBitAddress, maxBitAddress);
	}
	
	public boolean isApplicableTo(EntityRange er) {
		AABB erbb = er.getAabb();
		return isApplicableTo(
			erbb.minX, erbb.minY, erbb.maxX, erbb.maxY,
			er.getMinBitAddress(), er.getMaxBitAddress());
	}
	
	//// MessageSet implementation
	
	@Override public Iterator<Message> iterator() {
		return Collections.singletonList(this).iterator();
	}
	@Override public int size() { return 1; }
	@Override public MessageSet subsetApplicableTo(
		double minX, double minY, double maxX, double maxY,
		long minBitAddress, long maxBitAddress
	) {
		return isApplicableTo(minX, minY, maxX, maxY, minBitAddress, maxBitAddress) ? this : MessageSet.EMPTY;
	}
}
