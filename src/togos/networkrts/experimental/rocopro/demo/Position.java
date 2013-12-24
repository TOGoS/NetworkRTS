package togos.networkrts.experimental.rocopro.demo;

import java.io.Serializable;

public class Position implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public final int x, y;
	
	public Position( int x, int y ) {
		this.x = x;
		this.y = y;
	}
}
