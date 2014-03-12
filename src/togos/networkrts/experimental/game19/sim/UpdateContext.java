package togos.networkrts.experimental.game19.sim;

import togos.networkrts.experimental.game19.world.Message;

/**
 * Allows objects being updated to do stuff!
 */
public interface UpdateContext extends MessageSender, AsyncTaskStarter
{
	public void sendMessage( Message m );
	/** Enqueue a task to be run in parallel with the simulation */
	public void startAsyncTask( AsyncTask at );
}
