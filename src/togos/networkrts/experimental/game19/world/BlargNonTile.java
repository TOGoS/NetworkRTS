package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.util.BitAddressUtil;

public class BlargNonTile implements NonTile
{
	public final long referenceTime;
	public final double x, y, vx, vy; // For simplicity, velocity is meters per clock tick
	public final AABB absolutePhysicalAabb;
	public final long id;
	public final NonTileInternals<? super BlargNonTile> internals;
	
	public BlargNonTile(
		long id, long referenceTime, double x, double y, double vx, double vy,
		NonTileInternals<? super BlargNonTile> internals
	) {
		this.id = id & BitAddresses.ID_MASK;
		this.referenceTime = referenceTime;
		this.x = x; this.vx = vx;
		this.y = y; this.vy = vy;
		this.absolutePhysicalAabb = internals.getRelativePhysicalAabb().shiftedBy(x,y,0);
		this.internals = internals;
	}
	
	/** 'Centered cube bounding box' */
	static AABB ccbb( double x, double y, double diameter ) {
		double radius = diameter/2;
		return new AABB( x-radius, y-radius, -radius, x+radius, y+radius, +radius );
	}
	
	public static BlargNonTile create( int id, long referenceTime, double x, double y, NonTileInternals<? super BlargNonTile> behavior ) {
		return new BlargNonTile( id, referenceTime, x, y, 0, 0, behavior );
	}
	
	public static BlargNonTile create( int id, long referenceTime, double x, double y, ImageHandle image, float diameter, NonTileInternals<? super BlargNonTile> internals ) {
		return create( id, referenceTime, x, y, internals );
	}
	
	@Override public long getReferenceTime() { return referenceTime; }
	@Override public AABB getAabb() { return absolutePhysicalAabb; }
	@Override public long getBitAddress() {
		return BitAddresses.TYPE_NONTILE | internals.getNonTileAddressFlags() | id;
	}
	@Override public long getMinBitAddress() { return getBitAddress(); }
	@Override public long getMaxBitAddress() { return getBitAddress(); }
	@Override public long getNextAutoUpdateTime() { return internals.getNextAutoUpdateTime(); }

	@Override public double getX() { return x; }
	@Override public double getY() { return y; }
	@Override public double getVelocityX() { return vx; }
	@Override public double getVelocityY() { return vy; }
	@Override public Icon getIcon() { return internals.getIcon(); }
	
	@Override public AABB getAbsolutePhysicalAabb() { return absolutePhysicalAabb; }
	@Override public AABB getRelativePhysicalAabb() { return internals.getRelativePhysicalAabb(); }
	
	public BlargNonTile withInternals(NonTileInternals<? super BlargNonTile> behavior) {
		return new BlargNonTile(id, referenceTime, x, y, vx, vy, behavior);
	}
	
	public BlargNonTile withId(int id) {
		return new BlargNonTile(id, referenceTime, x, y, vx, vy, internals);
	}
	
	@Override public BlargNonTile withPositionAndVelocity(long referenceTime, double x, double y, double vx, double vy) {
		return new BlargNonTile(id, referenceTime, x, y, vx, vy, internals);
	}
	
	public BlargNonTile withVelocity(long referenceTime, double vx, double vy) {
		return withPositionAndVelocity(referenceTime, x, y, vx, vy);
	}
	
	public BlargNonTile withPosition(long referenceTime, double x, double y) {
		return withPositionAndVelocity(referenceTime, x, y, vx, vy);
	}
	
	public boolean isSpecificallyAddressedBy(Message m) {
		return m.isApplicableTo(this) && BitAddressUtil.rangeContains(m, getBitAddress());
	}
	
	// This might nt need to be part of the interface, since
	// withPositionAndVelocity exists
	protected BlargNonTile withUpdatedPosition(long newTime) {
		if( newTime == referenceTime || (vx == 0 && vy == 0) ) return this;
		
		double interval = newTime-referenceTime;
		return withPosition(newTime, x+vx*interval, y+vy*interval);
	}
	
	@Override public NonTile update(
		long time, int phase, World w, MessageSet incomingMessages, NonTileUpdateContext updateContext
	) {
		switch(phase) {
		case 1: return withUpdatedPosition(time);
		case 2: return internals.update(this, time, w, incomingMessages, updateContext);
		default:
			throw new RuntimeException("Unsupported update phase: "+phase);
		}
	}
}
