package togos.networkrts.experimental.gameengine1.index;

import togos.networkrts.util.BitAddressUtil;

public class EntityRanges
{
	public static final EntityRange BOUNDLESS = new EntityRange() {
		@Override public AABB getAabb() { return AABB.BOUNDLESS; }
		@Override public long getMaxBitAddress() { return BitAddressUtil.MAX_ADDRESS; }
		@Override public long getMinBitAddress() { return BitAddressUtil.MIN_ADDRESS; }
	};
	
	/**
	 * Returns true if all of AABBs, bit address ranges, and auto update time ranges intersect.
	 * 
	 * Auto update time is not checked.
	 */
	public static boolean intersects( EntityRange a, EntityRange b ) {
		return BitAddressUtil.rangesIntersect(a, b) && a.getAabb().intersects(b.getAabb());
	}

	public static EntityRange create(final AABB aabb, final long minBa, final long maxBa) {
		return new EntityRange() {
			@Override public AABB getAabb() { return aabb; }
			@Override public long getMinBitAddress() { return minBa; }
			@Override public long getMaxBitAddress() { return maxBa; }
		};
	}
	
	public static EntityRange forAabb(final AABB aabb) {
		return create(aabb, BitAddressUtil.MIN_ADDRESS, BitAddressUtil.MAX_ADDRESS);
	}
}
