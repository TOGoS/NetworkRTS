package togos.networkrts.awt;

import java.awt.Graphics2D;

/**
 * Any component that can paint itself onto a Graphics2D
 * given a timestamp.  The AWTPainter should not be concerned
 * with buffering, but may use the Graphics2D's clip to
 * eliminate unnecessary drawing calls.
 */
public interface AWTPaintable
{
	public void paint( Graphics2D g2d, long timestamp );
}
