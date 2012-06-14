package togos.networkrts.experimental.shape;

public class TBoundless implements RectIntersector
{
	public static final TBoundless INSTANCE = new TBoundless();
	
	protected TBoundless() { }
	
	@Override public int rectIntersection(double x, double y, double w, double h) { return RectIntersector.INCLUDES_ALL; }
}
