package togos.networkrts.experimental.game19.sim;

import togos.networkrts.experimental.game19.world.Message;

/**
 * Allows objects being updated to do stuff!
 */
public interface UpdateContext
{
	public void sendMessage( Message m );
}
