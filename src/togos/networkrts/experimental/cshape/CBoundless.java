package togos.networkrts.experimental.cshape;

public class CBoundless implements CShape
{
	public static final CBoundless INSTANCE = new CBoundless();
	
	protected CBoundless() { }
	
	@Override public int rectIntersection(double x, double y, double w, double h) { return CShape.INCLUDES_ALL; }
}
