package togos.networkrts.experimental.gensim;

/**
 * TimedEventHandler that also has internally-scheduled updates.
 */
public interface Stepper<EventClass> extends TimedEventHandler<EventClass>
{
	public long getNextInternalUpdateTime();
	public Stepper<EventClass> update( long time, EventClass evt ) throws Exception;
}
