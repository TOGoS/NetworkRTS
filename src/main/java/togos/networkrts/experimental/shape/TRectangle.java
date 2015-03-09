package togos.networkrts.experimental.shape;

public class TRectangle implements RectIntersector
{
	public final double x, y, w, h;
	
	public TRectangle( double x, double y, double w, double h ) {
		this.x = x; this.w = w;
		this.y = y; this.h = h;
	}
	
	@Override public int rectIntersection( double x, double y, double w, double h ) {
		if( x + w <= this.x || x >= this.x + this.w || y + h <= this.y || y >= this.y + this.h ) {
			return INCLUDES_NONE;
		}
		if( x >= this.x && y >= this.y && x + w <= this.x + this.w && y + h <= this.y + this.h ) {
			return INCLUDES_ALL;
		}
		return INCLUDES_SOME;
	}
}
