package togos.networkrts.experimental.netsim2;

import togos.networkrts.experimental.gensim.Timestamped;

public interface EventHandler
{
	public void eventOccured( Timestamped event ) throws Exception;
}
