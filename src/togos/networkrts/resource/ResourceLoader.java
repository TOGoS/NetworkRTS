package togos.networkrts.resource;

import togos.networkrts.world.Handle;

public class ResourceLoader
{
	public void precache( Handle h ) {
		
	}
	public Object getValue( Handle h, Object onNull ) {
		Object v = h.getValue();
		if( v != null ) return v;
		if( IDs.NULL_ID.equals(h.id) ) return onNull;
		throw new RuntimeException("Whups, I can't load anything yet!");
	}
}
