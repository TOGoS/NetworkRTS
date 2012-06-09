package togos.networkrts.experimental.netsim2;

import togos.networkrts.experimental.gensim.Timestamped;

public interface LiveEvent extends Timestamped
{
	public boolean isAlive( long timestamp );
}
