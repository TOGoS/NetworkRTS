package togos.networkrts.awt;

import java.awt.Graphics2D;

/**
 * Any component that can paint itself onto a Graphics2D
 * given a timestamp.  The AWTPainter should not be concerned
 * with buffering, but may use the Graphics2D's clip to
 * eliminate unnecessary drawing calls.
 */
public interface TimestampedPaintable
{
	/**
	 * @param timestamp draw the component as it appears at this time
	 * @param width total width of component
	 * @param height total height of component 
	 * @param g2d
	 */
	public void paint( long timestamp, int width, int height, Graphics2D g2d );
}
