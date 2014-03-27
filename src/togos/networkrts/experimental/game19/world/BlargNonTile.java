package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class BlargNonTile implements NonTile
{
	public final long referenceTime;
	public final double x, y, vx, vy; // For simplicity, velocity is meters per clock tick
	public final AABB absolutePhysicalAabb;
	public final int id;
	public final NonTileInternals<? super BlargNonTile> behavior;
	
	public BlargNonTile(
		int id, long referenceTime, double x, double y, double vx, double vy,
		NonTileInternals<? super BlargNonTile> behavior
	) {
		this.id = id;
		this.referenceTime = referenceTime;
		this.x = x; this.vx = vx;
		this.y = y; this.vy = vy;
		this.absolutePhysicalAabb = behavior.getRelativePhysicalAabb().shiftedBy(x,y,0);
		this.behavior = behavior;
	}
	
	/** 'Centered cube bounding box' */
	static AABB ccbb( double x, double y, double diameter ) {
		double radius = diameter/2;
		return new AABB( x-radius, y-radius, -radius, x+radius, y+radius, +radius );
	}
	
	public static BlargNonTile create( int id, long referenceTime, double x, double y, NonTileInternals<? super BlargNonTile> behavior ) {
		return new BlargNonTile( id, referenceTime, x, y, 0, 0, behavior );
	}

	public static BlargNonTile create( int id, long referenceTime, double x, double y, ImageHandle image, float diameter, NonTileInternals<? super BlargNonTile> behavior ) {
		return create( id, referenceTime, x, y, behavior );
	}
	
	@Override public long getReferenceTime() { return referenceTime; }
	@Override public AABB getAabb() { return absolutePhysicalAabb; }
	@Override public long getMinBitAddress() { return BitAddresses.TYPE_NONTILE | id; }
	@Override public long getMaxBitAddress() { return BitAddresses.TYPE_NONTILE | id; }
	@Override public long getNextAutoUpdateTime() { return behavior.getNextAutoUpdateTime(); }

	@Override public double getX() { return x; }
	@Override public double getY() { return y; }
	@Override public double getVelocityX() { return vx; }
	@Override public double getVelocityY() { return vy; }
	@Override public Icon getIcon() { return behavior.getIcon(); }
	
	@Override public AABB getAbsolutePhysicalAabb() { return absolutePhysicalAabb; }
	@Override public AABB getRelativePhysicalAabb() { return behavior.getRelativePhysicalAabb(); }
	
	public BlargNonTile withInternals(NonTileInternals<? super BlargNonTile> behavior) {
		return new BlargNonTile(id, referenceTime, x, y, vx, vy, behavior);
	}
	
	public BlargNonTile withId(int id) {
		return new BlargNonTile(id, referenceTime, x, y, vx, vy, behavior);
	}
	
	@Override public BlargNonTile withPositionAndVelocity(long referenceTime, double x, double y, double vx, double vy) {
		return new BlargNonTile(id, referenceTime, x, y, vx, vy, behavior);
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
