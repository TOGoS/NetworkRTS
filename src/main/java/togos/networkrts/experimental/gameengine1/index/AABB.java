package togos.networkrts.experimental.gameengine1.index;

import togos.networkrts.experimental.shape.RectIntersector;

/** Axis-aligned bounding box */
public class AABB implements RectIntersector
{
	public static final AABB BOUNDLESS = new AABB(
		Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
		Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY
	);
	
	public final double minX, minY, minZ, maxX, maxY, maxZ;
	
	public AABB( double x0, double y0, double z0, double x1, double y1, double z1 ) {
		assert x0 < x1;
		assert y0 < y1;
		assert z0 < z1;
		minX = x0;  maxX = x1;
		minY = y0;  maxY = y1;
		minZ = z0;  maxZ = z1;
	}
	
	public AABB(AABB o) {
		this( o.minX, o.minY, o.minZ, o.maxX, o.maxY, o.maxZ );
	}
	
	public final AABB centered( double x, double y, double z, double rad ) {
		return new AABB( x-rad, y-rad, z-rad, x+rad, y+rad, z+rad );
	}
	
	protected final boolean contains( double ox0, double oy0, double oz0, double ox1, double oy1, double oz1 ) {
		if( ox0 < minX ) return false;
		if( oy0 < minY ) return false;
		if( oz0 < minZ ) return false;
		if( ox1 > maxX ) return false;
		if( oy1 > maxY ) return false;
		if( oz1 > maxZ ) return false;
		return true;
	}
	
	public final boolean contains( AABB other ) {
		return contains( other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ );
	}
	
	protected final boolean intersects( double ox0, double oy0, double oz0, double ox1, double oy1, double oz1 ) {
		if( maxX <= ox0 ) return false;
		if( maxY <= oy0 ) return false;
		if( maxZ <= oz0 ) return false;
		if( minX >= ox1 ) return false;
		if( minY >= oy1 ) return false;
		if( minZ >= oz1 ) return false;
		return true;
	}
	
	public final boolean intersects( AABB other ) {
		return intersects( other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ );
	}
	
	protected static boolean isFinite( double v ) {
		return !Double.isNaN(v) && !Double.isInfinite(v);
	}
	
	public boolean isFinite() {
		return
			isFinite(minX) && isFinite(minY) && isFinite(minZ) &&
			isFinite(maxX) && isFinite(maxY) && isFinite(maxZ);
	}
	
	public final AABB shiftedBy( double dx, double dy, double dz ) {
		return new AABB(minX+dx, minY+dy, minZ+dz, maxX+dx, maxY+dy, maxZ+dz);
	}
	
	@Override public int rectIntersection(double x, double y, double w, double h) {
		return
			contains(x, y, minZ, x+w, y+h, maxZ) ? INCLUDES_ALL :
			intersects(x, y, minZ, x+w, y+h, maxZ) ? INCLUDES_SOME :
			INCLUDES_NONE;
	}
	
	public String toString() {
		return String.format(
			"AABB (%.2f, %.2f, %.2f) to (%.2f, %.2f, %.2f)",
			minX, minY, minZ, maxX, maxY, maxZ
		);
	}
}
