package togos.networkrts.experimental.cshape;

public class CUnion implements CShape
{
	public final CShape[] subShapes;
	
	public CUnion( CShape[] subShapes ) {
		this.subShapes = subShapes;
	}
	
	public int rectIntersection(double x, double y, double w, double h) {
		int inclusiveness = 0;
		for( int i=0; i<subShapes.length; ++i ) {
			int subInc = subShapes[i].rectIntersection(x, y, w, h);
			if( subInc == CShape.INCLUDES_ALL ) return subInc;
			inclusiveness |= subInc;
		}
		return inclusiveness;
	}
}
