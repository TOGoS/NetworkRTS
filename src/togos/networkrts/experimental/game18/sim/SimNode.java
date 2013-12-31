package togos.networkrts.experimental.game18.sim;

import java.util.List;

public interface SimNode
{
	public long getMinId();
	public long getMaxId();
	public long getNextAutoUpdateTime();
	/**
	 * Updating based on timestamp update
	 * and message is combined to simplify implementation.
	 * 
	 * Message may not be null, but it may be Message.NONE.
	 * 
	 * @param rootNode root node of the simulation under which other nodes may be found
	 * @param timestamp time to update to
	 * @param m message to be processed
	 * @param messageDest list to which to append new messages for immediate delivery
	 * @return the new version of this object, or null to have it removed
	 */
	public SimNode update( SimNode rootNode, long timestamp, Message m, List<Message> messageDest );
	public <T> T get( long id, Class<T> expectedClass );
}
