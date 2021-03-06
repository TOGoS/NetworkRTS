package togos.networkrts.experimental.game19.world;

import java.util.Collections;
import java.util.Iterator;

import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityRange;
import togos.networkrts.experimental.shape.RectIntersector;
import togos.networkrts.experimental.shape.TBoundless;
import togos.networkrts.util.BitAddressRange;
import togos.networkrts.util.BitAddressUtil;

public class Message implements BitAddressRange, MessageSet
{
	public static final Message[] EMPTY_LIST = new Message[0];
	
	public enum MessageType {
		UPDATE, // Used to force an update
		INCOMING_PACKET,
		REQUEST_PICKUP, // Somebody wants to take you!  Remove self and respond with INCOMING_ITEM if successful
		INCOMING_ITEM, // Payload will be a NonTileInternals
		ADD_BLOCKS,
		REPLACE_BLOCKS,
	}
	
	public final long minBitAddress, maxBitAddress;
	public final RectIntersector targetShape;
	public final MessageType type;
	public final long sourceAddress;
	public final Object payload;
	
	public Message( long minBa, long maxBa, RectIntersector targetShape, MessageType type, long sourceAddress, Object payload ) {
		this.minBitAddress = minBa; this.maxBitAddress = maxBa;
		this.targetShape = targetShape;
		this.type = type;
		this.sourceAddress = sourceAddress;
		this.payload = payload;
		if( (minBa & 0xFFFF000000000000l) == 0x0005000000000000l) {
			//System.err.println("WAT");
		}
	}
	
	public static Message create( long minBa, long maxBa, MessageType type, long sourceAddress, Object payload ) {
		return new Message( minBa, maxBa, TBoundless.INSTANCE, type, sourceAddress, payload );
	}
	
	public static Message create( long minBa, long maxBa, MessageType type, Object payload ) {
		return create( minBa, maxBa, type, BitAddressUtil.NO_ADDRESS, payload );
	}
	
	public static Message create( long targetBa, RectIntersector targetShape, MessageType type, long sourceAddress, Object payload ) {
		return new Message( BitAddresses.withMinFlags(targetBa), BitAddresses.withMaxFlags(targetBa), targetShape, type, sourceAddress, payload );
	}
	
	public static Message create( long targetBa, RectIntersector targetShape, MessageType type, Object payload ) {
		return create( targetBa, targetShape, type, BitAddressUtil.NO_ADDRESS, payload );
	}
	
	public static Message create( long targetBa, MessageType type, Object payload ) {
		return create( targetBa, TBoundless.INSTANCE, type, BitAddressUtil.NO_ADDRESS, payload );
	}
	
	public static Message create( long targetBa, MessageType type, long sourceAddress, Object payload ) {
		return new Message( BitAddresses.withMinFlags(targetBa), BitAddresses.withMaxFlags(targetBa), TBoundless.INSTANCE, type, sourceAddress, payload );
	}
	
	public static Message create( NonTile target, MessageType type, long sourceAddress, Object payload ) {
		return new Message( target.getBitAddress(), target.getBitAddress(), target.getAabb(), type, sourceAddress, payload );
	}
	
	@Override public long getMinBitAddress() { return minBitAddress; }
	@Override public long getMaxBitAddress() { return maxBitAddress; }
	
	public boolean isApplicableTo(
		double minX, double minY, double maxX,
		double maxY, long minBitAddress, long maxBitAddress
	) {
		return
			targetShape.rectIntersection(minX, minY, maxX-minX, maxY-minY) != RectIntersector.INCLUDES_NONE &&
			BitAddressUtil.rangesIntersect(this, minBitAddress, maxBitAddress);
	}
	
	public boolean isApplicableTo(EntityRange er) {
		AABB erbb = er.getAabb();
		return isApplicableTo(
			erbb.minX, erbb.minY, erbb.maxX, erbb.maxY,
			er.getMinBitAddress(), er.getMaxBitAddress());
	}
	
	public Message withSourceAddress(long sourceAddress) {
		return new Message(minBitAddress, maxBitAddress, targetShape, type, sourceAddress, payload);
	}
	
	public String toString() {
		return
			"Message to "+BitAddresses.toString(minBitAddress)+".."+BitAddresses.toString(maxBitAddress)+
			" from "+BitAddresses.toString(sourceAddress)+" type "+type+" payload "+(payload == null ? null : payload.toString()) +
			" target shape "+targetShape;
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
