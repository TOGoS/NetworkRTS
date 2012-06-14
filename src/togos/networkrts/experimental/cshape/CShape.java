package togos.networkrts.experimental.cshape;

public interface CShape
{
	public static final int INCLUDES_NONE = 0;
	public static final int INCLUDES_SOME = 1;
	public static final int INCLUDES_ALL  = 2;
	
	/** Does this shape contain none, some, or all of the given rectangle? */ 
	public int rectIntersection( double x, double y, double w, double h );
}
