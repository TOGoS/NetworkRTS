package togos.networkrts.s64;

public interface Shape
{
	public static final int INCLUDES_NONE = 0;
	public static final int INCLUDES_SOME = 1;
	public static final int INCLUDES_ALL  = 2;
	
	/** Does this shape contain none, some, or all of the given rectangle? */ 
	public int includes( double x, double y, double w, double h );
}
