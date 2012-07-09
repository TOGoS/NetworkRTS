package togos.networkrts.experimental.gensim3;

import java.util.Collection;

public interface Simulatable
{
	/**
	 * Return the next timestamp that should be advancedTo provided 
	 * no other external events occur.
	 */
	public long getNextInternalEventTimestamp();
	public Simulatable advanceTo( long timestamp, Collection outputEvents );
	public Simulatable handle( Object inputEvent, Collection outputEvents );
}
