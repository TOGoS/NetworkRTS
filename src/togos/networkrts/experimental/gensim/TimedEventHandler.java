package togos.networkrts.experimental.gensim;

/**
 * Generally used to represents the state of a simulation at a specific time.
 */
public interface TimedEventHandler<EventClass>
{
	/**
	 * Forward the simulation to time time and then process evt, if non-null, at that time.
	 * May throw an exception if the current state is already > time.
	 * Returns an updated version of itself, which may be the same object after modifications,
	 * or a newly created object.
	 */
	public TimedEventHandler<EventClass> update( long time, EventClass evt ) throws Exception;
}
