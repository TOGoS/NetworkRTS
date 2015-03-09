package togos.networkrts.tfunc;

import java.awt.Color;

public interface ColorFunction
{
	public int getColor(long ts);
	public Color getAwtColor(long ts);
}
