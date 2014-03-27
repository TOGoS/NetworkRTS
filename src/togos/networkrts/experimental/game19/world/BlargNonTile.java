package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class BlargNonTile implements NonTile
{
	public final long referenceTime;
	public final double x, y, vx, vy; // For simplicity, velocity is meters per clock tick
	public final AABB relativePhysicalAabb;
	public final AABB absolutePhysicalAabb;
	public final int id;
	public final long nextAutoUpdateTime;
	public final Icon icon;
	public final NonTileBehavior<? super BlargNonTile> behavior;
	
	public BlargNonTile(
		long referenceTime, double x, double y, double vx, double vy,
		AABB relativeAabb, int id, long nextAut,
		Icon icon, NonTileBehavior<? super BlargNonTile> behavior
	) {
		this.referenceTime = referenceTime;
		this.x = x; this.vx = vx;
		this.y = y; this.vy = vy;
		this.relativePhysicalAabb = relativeAabb;
		this.absolutePhysicalAabb = relativeAabb.shiftedBy(x,y,0);
		this.id = id;
		this.nextAutoUpdateTime = nextAut;
		this.icon = icon;
		this.behavior = behavior;
	}
	
	/** 'Centered cube bounding box' */
	static AABB ccbb( double x, double y, double diameter ) {
		double radius = diameter/2;
		return new AABB( x-radius, y-radius, -radius, x+radius, y+radius, +radius );
	}
	
	public static BlargNonTile create( long referenceTime, double x, double y, Icon icon, float diameter, NonTileBehavior<? super BlargNonTile> behavior ) {
		return new BlargNonTile( referenceTime, x, y, 0, 0, ccbb(x,y,diameter), 0, Long.MAX_VALUE, icon, behavior );
	}

	public static BlargNonTile create( long referenceTime, double x, double y, ImageHandle image, float diameter, NonTileBehavior<? super BlargNonTile> behavior ) {
		Icon icon = new Icon( image, -diameter/2, -diameter/2, Icon.DEFAULT_NONTILE_FRONT_Z, diameter, diameter );
		return create( referenceTime, x, y, icon, diameter, behavior );
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static BlargNonTile create( long referenceTime, double x, double y, ImageHandle image, float diameter ) {
		return create( referenceTime, x, y, image, diameter, (NonTileBehavior<BlargNonTile>)(NonTileBehavior)NonTileBehavior.NONE );
	}
	
	@Override public long getReferenceTime() { return referenceTime; }
	@Override public AABB getAabb() { return absolutePhysicalAabb; }
	@Override public long getMinBitAddress() { return BitAddresses.TYPE_NONTILE | id; }
	@Override public long getMaxBitAddress() { return BitAddresses.TYPE_NONTILE | id; }
	@Override public long getNextAutoUpdateTime() { return nextAutoUpdateTime; }

	@Override public double getX() { return x; }
	@Override public double getY() { return y; }
	@Override public double getVelocityX() { return vx; }
	@Override public double getVelocityY() { return vy; }
	@Override public Icon getIcon() { return icon; }
	
	@Override public AABB getAbsolutePhysicalAabb() { return absolutePhysicalAabb; }
	@Override public AABB getRelativePhysicalAabb() { return relativePhysicalAabb; }
	
	public BlargNonTile withBehavior(NonTileBehavior<? super BlargNonTile> behavior) {
		return new BlargNonTile(
			referenceTime, x, y, vx, vy, relativePhysicalAabb,
			id, nextAutoUpdateTime,
			icon, behavior
		);
	}
	
	public BlargNonTile withId(int id) {
		return new BlargNonTile(
			referenceTime, x, y, vx, vy, relativePhysicalAabb,
			id, nextAutoUpdateTime,
			icon, behavior
		);
	}
	
	public BlargNonTile withIcon(Icon icon) {
		return new BlargNonTile(
			referenceTime, x, y, vx, vy, relativePhysicalAabb,
			id, nextAutoUpdateTime,
			icon, behavior
		);
	}
	
	@Override public BlargNonTile withPositionAndVelocity(long referenceTime, double x, double y, double vx, double vy) {
		return new BlargNonTile(
			referenceTime, x, y, vx, vy, relativePhysicalAabb,
			id, nextAutoUpdateTime,
			icon, behavior
		);
	}
	
	public BlargNonTile withVelocity(long referenceTime, double vx, double vy) {
		return withPositionAndVelocity(referenceTime, x, y, vx, vy);
	}
	
	public BlargNonTile withPosition(long referenceTime, double x, double y) {
		return withPositionAndVelocity(referenceTime, x, y, vx, vy);
	}
	
	// This might nt need to be part of the interface, since
	// withPositionAndVelocity exists
	@Override public BlargNonTile withUpdatedPosition(long newTime) {
		if( newTime == referenceTime || (vx == 0 && vy == 0) ) return this;
		
		double interval = newTime-referenceTime;
		return withPosition(newTime, x+vx*interval, y+vy*interval);
	}
	
	@Override public NonTile update(
		long time, World w, MessageSet incomingMessages, NonTileUpdateContext updateContext
	) {
		return behavior.update(this, time, w, incomingMessages, updateContext);
	}
}
