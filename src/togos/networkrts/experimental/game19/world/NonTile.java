package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityRange;
import togos.networkrts.util.BitAddressUtil;

public class NonTile implements EntityRange
{
	public final long referenceTime;
	public final double x, y, vx, vy; // For simplicity, velocity is meters per clock tick
	public final AABB relativePhysicalAabb;
	public final AABB absolutePhysicalAabb;
	public final long minBitAddress, maxBitAddress;
	public final long nextAutoUpdateTime;
	public final Icon icon;
	public final NonTileBehavior behavior;
	
	public NonTile(
		long referenceTime, double x, double y, double vx, double vy,
		AABB physicalAabb, long minBa, long maxBa, long nextAut,
		Icon icon, NonTileBehavior behavior
	) {
		this.referenceTime = referenceTime;
		this.x = x; this.vx = vx;
		this.y = y; this.vy = vy;
		this.relativePhysicalAabb = physicalAabb;
		this.absolutePhysicalAabb = physicalAabb.shiftedBy(x,y,0);
		this.minBitAddress = minBa;
		this.maxBitAddress = maxBa;
		this.nextAutoUpdateTime = nextAut;
		this.icon = icon;
		this.behavior = behavior;
	}
	
	/** 'Centered cube bounding box' */
	static AABB ccbb( double x, double y, double diameter ) {
		double radius = diameter/2;
		return new AABB( x-radius, y-radius, -radius, x+radius, y+radius, +radius );
	}
	
	public static NonTile create( long referenceTime, double x, double y, Icon icon, float diameter, NonTileBehavior behavior ) {
		return new NonTile( referenceTime, x, y, 0, 0, ccbb(x,y,diameter), BitAddressUtil.MAX_ADDRESS, BitAddressUtil.MIN_ADDRESS, Long.MAX_VALUE, icon, behavior );
	}

	public static NonTile create( long referenceTime, double x, double y, ImageHandle image, float diameter, NonTileBehavior behavior ) {
		Icon icon = new Icon( image, -diameter/2, -diameter/2, Icon.DEFAULT_NONTILE_FRONT_Z, diameter, diameter );
		return create( referenceTime, x, y, icon, diameter, behavior );
	}
	
	public static NonTile create( long referenceTime, double x, double y, ImageHandle image, float diameter ) {
		return create( referenceTime, x, y, image, diameter, NonTileBehavior.NONE );
	}
	
	@Override public AABB getAabb() { return absolutePhysicalAabb; }
	@Override public long getMinBitAddress() { return minBitAddress; }
	@Override public long getMaxBitAddress() { return maxBitAddress; }
	@Override public long getNextAutoUpdateTime() { return nextAutoUpdateTime; }
	
	public NonTile withBehavior(NonTileBehavior behavior) {
		return new NonTile(
			referenceTime, x, y, vx, vy, relativePhysicalAabb,
			minBitAddress, maxBitAddress, nextAutoUpdateTime,
			icon, behavior
		);
	}
	
	public NonTile withIdRange(long minBa, long maxBa) {
		return new NonTile(
			referenceTime, x, y, vx, vy, relativePhysicalAabb,
			minBa, maxBa, nextAutoUpdateTime,
			icon, behavior
		);
	}
	
	public NonTile withId(int id) {
		return withIdRange(BitAddresses.withMinFlags(id), BitAddresses.withMaxFlags(id));
	}
	
	public NonTile withIcon(Icon icon) {
		return new NonTile(
			referenceTime, x, y, vx, vy, relativePhysicalAabb,
			minBitAddress, maxBitAddress, nextAutoUpdateTime,
			icon, behavior
		);
	}
	
	public NonTile withPositionAndVelocity(long referenceTime, double x, double y, double vx, double vy) {
		return new NonTile(
			referenceTime, x, y, vx, vy, relativePhysicalAabb,
			minBitAddress, maxBitAddress, nextAutoUpdateTime,
			icon, behavior
		);
	}
	
	public NonTile withVelocity(long referenceTime, double vx, double vy) {
		return withPositionAndVelocity(referenceTime, x, y, vx, vy);
	}
	
	public NonTile withPosition(long referenceTime, double x, double y) {
		return withPositionAndVelocity(referenceTime, x, y, vx, vy);
	}
	
	public NonTile withUpdatedPosition(long newTime) {
		if( newTime == referenceTime || (vx == 0 && vy == 0) ) return this;
		
		double interval = newTime-referenceTime;
		return withPosition(newTime, x+vx*interval, y+vy*interval);
	}
}
