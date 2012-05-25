package togos.networkrts.experimental.s64;

public class UnionShape implements Shape
{
	public final Shape[] subShapes;
	
	public UnionShape( Shape[] subShapes ) {
		this.subShapes = subShapes;
	}
	
	public int includes(double x, double y, double w, double h) {
		int inclusiveness = 0;
		for( int i=0; i<subShapes.length; ++i ) {
			int subInc = subShapes[i].includes(x, y, w, h);
			if( subInc == Shape.INCLUDES_ALL ) return subInc;
			inclusiveness |= subInc;
		}
		return inclusiveness;
	}
}
