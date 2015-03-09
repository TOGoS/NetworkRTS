package togos.networkrts.experimental.simpleclient;

import java.io.Serializable;

public class BackgroundArea implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public int minX, minY, maxX, maxY;
	public BackgroundType type;
}
