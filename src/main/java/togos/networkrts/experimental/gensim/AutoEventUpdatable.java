package togos.networkrts.experimental.gensim;

import togos.networkrts.util.HasNextAutoUpdateTime;

/**
 * EventUpdatable that also has internally-scheduled updates.
 */
public interface AutoEventUpdatable<EventClass> extends EventUpdatable<EventClass>, HasNextAutoUpdateTime
{
	static final long TIME_IMMEDIATE = Long.MIN_VALUE;
	static final long TIME_INFINITY = Long.MAX_VALUE;
	
	public AutoEventUpdatable<EventClass> update( long time, EventClass evt ) throws Exception;
}
