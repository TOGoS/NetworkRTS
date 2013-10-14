package togos.networkrts.experimental.dungeon;

public interface UpdateListener
{
	/**
	 * "Something changed that may have affected you!"
	 * 
	 * This may schedule new message deliveries, but may not
	 * schedule updates.
	 */
	public void updated();
}
