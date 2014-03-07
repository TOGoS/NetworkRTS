package togos.networkrts.experimental.gameengine1.index;

/**
 * An object defined by a bounding box, optional tag, and flags.
 */
public class BaseEntity extends AABB implements EntityRange
{
	/** Can be used to reference an entity regardless of its state */ 
	public final long minBitAddress;
	public final long maxBitAddress;
	public final long nextAutoUpdateTime;
	
	public BaseEntity( long minBitAddress, long maxBitAddress, long nextAutoUpdateTime, double minX, double minY, double minZ, double maxX, double maxY, double maxZ ) {
		super( minX, minY, minZ, maxX, maxY, maxZ );
		this.minBitAddress = minBitAddress;
		this.maxBitAddress = maxBitAddress;
		this.nextAutoUpdateTime = nextAutoUpdateTime;
	}
	
	@Override public AABB getAABB() { return this; }
	@Override public long getMaxBitAddress() { return maxBitAddress; }
	@Override public long getMinBitAddress() { return minBitAddress; }
	@Override public long getNextAutoUpdateTime() { return nextAutoUpdateTime; }
}
