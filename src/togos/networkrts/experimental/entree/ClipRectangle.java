package togos.networkrts.experimental.entree;

public class ClipRectangle implements ClipShape
{
	public final double x, y, w, h;
	
	public ClipRectangle( double x, double y, double w, double h ) {
		this.x = x; this.y = y;
		this.w = w; this.h = h;
	}
	
	public boolean intersectsRect( double x, double y, double w, double h ) {
		if( x + w <= this.x ) return false;
		if( y + h <= this.y ) return false;
		if( x >= this.x + this.w ) return false;
		if( y >= this.y + this.h ) return false;
		return true;
	}
}
