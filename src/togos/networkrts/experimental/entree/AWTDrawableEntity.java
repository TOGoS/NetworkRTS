package togos.networkrts.experimental.entree;

import java.awt.Graphics2D;

public interface AWTDrawableEntity
{
	public double getX();
	public double getY();
	public void draw( Graphics2D g2d, double x, double y, double scale );
}
