package togos.networkrts.experimental.entree;

import java.awt.Graphics2D;

public interface AWTDrawableEntity
{
	public double getX();
	public double getY();

	/**
	 * renerLayer is the layer number to draw; an entity should only draw parts of
	 * itself that are on this layer.
	 */
	public void draw( Graphics2D g2d, double x, double y, double scale, long timestamp, int renderLayer );
}
