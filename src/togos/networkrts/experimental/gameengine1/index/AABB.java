package togos.networkrts.experimental.gameengine1.index;

/** Axis-aligned bounding box */
public class AABB {
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
	
	public final boolean contains( AABB other ) {
		if( other.minX < minX ) return false;
		if( other.minY < minY ) return false;
		if( other.minZ < minZ ) return false;
		if( other.maxX > maxX ) return false;
		if( other.maxY > maxY ) return false;
		if( other.maxZ > maxZ ) return false;
		return true;
	}
	
	public final boolean intersects( AABB other ) {
		if( maxX < other.minX ) return false;
		if( maxY < other.minY ) return false;
		if( maxZ < other.minZ ) return false;
		if( minX > other.maxX ) return false;
		if( minY > other.maxY ) return false;
		if( minZ > other.maxZ ) return false;
		return true;
	}
	
	protected static boolean isFinite( double v ) {
		return !Double.isNaN(v) && !Double.isInfinite(v);
	}
	
	public boolean isFinite() {
		return
			isFinite(minX) && isFinite(minY) && isFinite(minZ) &&
			isFinite(maxX) && isFinite(maxY) && isFinite(maxZ);
	}
}