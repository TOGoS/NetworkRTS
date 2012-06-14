package togos.networkrts.experimental.shape;

public class TUnion implements RectIntersector
{
	public final RectIntersector[] subShapes;
	
	public TUnion( RectIntersector[] subShapes ) {
		this.subShapes = subShapes;
	}
	
	public int rectIntersection(double x, double y, double w, double h) {
		int inclusiveness = 0;
		for( int i=0; i<subShapes.length; ++i ) {
			int subInc = subShapes[i].rectIntersection(x, y, w, h);
			if( subInc == RectIntersector.INCLUDES_ALL ) return subInc;
			inclusiveness |= subInc;
		}
		return inclusiveness;
	}
}
