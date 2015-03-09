package togos.networkrts.experimental.gensim;

/**
 * Generally used to represents the state of a simulation at a specific time.
 * The state of the simulation can be updated by time passing or by
 * external events occurring.
 */
public interface EventUpdatable<EventClass>
{
	/**
	 * Forward the simulation to time time and then process event, if non-null, at that time.
	 * Should throw an exception if the current state is already > time.
	 * Returns an updated version of itself, which may be the same object after modifications,
	 * or a newly created object.
	 */
	public EventUpdatable<EventClass> update( long time, EventClass event ) throws Exception;
}
