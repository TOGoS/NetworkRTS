package togos.networkrts.experimental.entree;

import java.awt.Graphics2D;

/**
 * An object that can draw itself at an arbitrary position and rotation.
 */
public interface AWTDrawable
{
	/**
	 * renderLayer is the layer number to draw; an entity should only draw parts of
	 * itself that are on this layer.
	 * 
	 * If this object has a position and rotation (implements WorldPositioned),
	 * that position and rotation will already be taken into account by the caller.
	 */
	public void draw( Graphics2D g2d, double x, double y, double scale, double rotation, long timestamp, int renderLayer );
}
