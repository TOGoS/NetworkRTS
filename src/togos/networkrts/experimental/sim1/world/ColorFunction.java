package togos.networkrts.experimental.sim1.world;

import java.awt.Color;

public interface ColorFunction
{
	public int getColor(int ts);
	public Color getAwtColor(int ts);
}
