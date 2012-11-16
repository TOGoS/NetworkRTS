package togos.networkrts.tfunc;

import java.awt.Color;

public class ConstantColorFunction extends Color implements ColorFunction
{
	private static final long serialVersionUID = 4544603749564664563L;

	public ConstantColorFunction( int argb ) { super(argb);       }
	public ConstantColorFunction( Color c  ) {  this(c.getRGB()); }
	
	public int getColor(long ts)      { return getRGB(); }
	public Color getAwtColor(long ts) { return this;     }
}
