package togos.networkrts.experimental.gensim;

import java.util.Collection;

import togos.networkrts.util.HasNextAutoUpdateTime;

public interface AutoEventUpdatable2<EventClass> extends HasNextAutoUpdateTime {
	public long getCurrentTime();
	public AutoEventUpdatable2<EventClass> update( long time, Collection<EventClass> events );
}
