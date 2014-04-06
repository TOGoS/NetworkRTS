package togos.networkrts.experimental.game19.sim;

import togos.networkrts.experimental.game19.util.MessageSender;

/**
 * Allows objects being updated to do stuff!
 */
public interface UpdateContext extends MessageSender, AsyncTaskStarter
{
	/** Enqueue a task to be run in parallel with the simulation */
	public void startAsyncTask( AsyncTask at );
}
