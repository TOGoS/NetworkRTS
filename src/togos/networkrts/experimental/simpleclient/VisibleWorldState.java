package togos.networkrts.experimental.simpleclient;

import java.io.Serializable;

/**
 * Contains information about the world that can be seen by a unit,
 * possibly to be communicated elsewhere
 */
public class VisibleWorldState implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public static VisibleWorldState BLANK = new VisibleWorldState( new BackgroundArea[0] );
	
	final BackgroundArea[] visibleBackgroundAreas;
	
	public VisibleWorldState( BackgroundArea[] vba ) {
		this.visibleBackgroundAreas = vba;
	}
}
