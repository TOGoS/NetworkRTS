package togos.networkrts.experimental.shape;

public class TCircle implements RectIntersector
{
	final double cx, cy, rad;
	
	public TCircle( double cx, double cy, double rad ) {
		this.cx = cx;
		this.cy = cy;
		this.rad = rad;
	}
	
	protected double distSquared( double x1, double y1, double x2, double y2 ) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		return dx*dx + dy*dy;
	}
	
	protected boolean includesCorner( double x, double y ) {
		return distSquared( x, y, cx, cy ) <= rad*rad;
	}
	
	protected int cornerQuadrant( double x, double y ) {
		if( y < cy ) {
			return x < cx ? 0 : 1;
		} else {
			return x < cx ? 2 : 3;
		}
	}
	
	public int rectIntersection( double x, double y, double w, double h ) {
		if( x+w <= cx - rad ) return RectIntersector.INCLUDES_NONE;
		if( x   >= cx + rad ) return RectIntersector.INCLUDES_NONE;
		if( y+h <= cy - rad ) return RectIntersector.INCLUDES_NONE;
		if( y   >= cy + rad ) return RectIntersector.INCLUDES_NONE;
		
		boolean includesSomeCorners = false;
		boolean includesAllCorners = true;
		
		if( includesCorner(x  ,y  ) ) {
			includesSomeCorners  = true;
		} else {
			includesAllCorners = false;
		}
		if( includesCorner(x+w,y  ) ) {
			includesSomeCorners  = true;
		} else {
			includesAllCorners = false;
		}
		if( includesCorner(x  ,y+h) ) {
			includesSomeCorners  = true;
		} else {
			includesAllCorners = false;
		}
		if( includesCorner(x+w,y+h) ) {
			includesSomeCorners  = true;
		} else {
			includesAllCorners = false;
		}
		
		// All corners in => entire area in
		if( includesAllCorners ) return RectIntersector.INCLUDES_ALL;
		// Some corners in => some of area in
		if( includesSomeCorners ) return RectIntersector.INCLUDES_SOME;
		
		// If the circle includes no corners but the corners are in different quadrants,
		// then parts the area must overlap the circle.
		// If all corners are in the same quadrant, then the entire area is outside the circle.
		return cornerQuadrant(x,y) == cornerQuadrant(x+w,y+h) ? RectIntersector.INCLUDES_NONE : RectIntersector.INCLUDES_SOME; 
	}
}
