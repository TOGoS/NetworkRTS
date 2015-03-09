package togos.networkrts.experimental.shape;

public interface RectIntersector
{
	public static final int INCLUDES_NONE = 0;
	public static final int INCLUDES_SOME = 1;
	public static final int INCLUDES_ALL  = 2;
	
	/**
	 * Returns one of the INCLUDE_ values depending on how much area
	 * of the given rectangle overlaps this shape.  Edges touching
	 * does not count as overlap!
	 * 
	 * Behavior is undefined if width or height are zero or negative.
	 * 
	 * @return
	 *   INCLUDE_NONE if the given rectangle is entirely outside of this shape,
	 *   INCLUDE_SOME if the given rectangle is partly inside of this shape, or
	 *   INCLUDE_ALL if the given rectangle is entirely inside of this shape
	 */ 
	public int rectIntersection( double x, double y, double w, double h );
}
