package togos.networkrts.tfunc;

import java.awt.Color;

public class ConstantColorFunction implements ColorFunction
{
	Color c;
	
	public ConstantColorFunction( Color c ) {
		this.c = c;
	}
	
	public int getColor(long ts) {
		return c.getRGB();
	}

	public Color getAwtColor(long ts) {
		return c;
	}
}
