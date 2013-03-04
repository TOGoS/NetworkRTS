package togos.networkrts.experimental.gensim;

/**
 * TimedEventHandler that also has internally-scheduled updates.
 */
public interface Stepper<EventClass> extends TimedEventHandler<EventClass>
{
	static final long TIME_INFINITY = Long.MAX_VALUE; 
	
	public long getNextInternalUpdateTime();
	public Stepper<EventClass> update( long time, EventClass evt ) throws Exception;
}
