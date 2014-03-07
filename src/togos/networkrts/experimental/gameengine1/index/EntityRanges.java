package togos.networkrts.experimental.gameengine1.index;

import togos.networkrts.util.BitAddressUtil;

public class EntityRanges
{
	public static final EntityRange BOUNDLESS = new EntityRange() {
		@Override public AABB getAABB() { return AABB.BOUNDLESS; }
		@Override public long getMaxBitAddress() { return BitAddressUtil.MAX_ADDRESS; }
		@Override public long getMinBitAddress() { return BitAddressUtil.MIN_ADDRESS; }
		@Override public long getNextAutoUpdateTime() { return Long.MAX_VALUE; }
	};
	
	/**
	 * Returns true if all of AABBs, bit address ranges, and auto update time ranges intersect.
	 * 
	 * Auto update time ranges are 0..a.nextAutoUpdateTime and b.nextAutoUpdateTime..infinity,
	 * so argument order is important.
	 */
	public static boolean intersects( EntityRange a, EntityRange b ) {
		return BitAddressUtil.rangesIntersect(a, b) && a.getAABB().intersects(b.getAABB()); // && a.getNextAutoUpdateTime() <= b.getNextAutoUpdateTime();
	}

	public static EntityRange forAABB(final AABB aabb) {
		return new EntityRange() {
			@Override public AABB getAABB() { return aabb; }
			@Override public long getMaxBitAddress() { return BitAddressUtil.MAX_ADDRESS; }
			@Override public long getMinBitAddress() { return BitAddressUtil.MIN_ADDRESS; }
			@Override public long getNextAutoUpdateTime() { return Long.MAX_VALUE; }
		};
	}
}
