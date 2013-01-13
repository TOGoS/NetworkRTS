package togos.networkrts.experimental.gensim;

/**
 * Single-threaded timestamped event handler.
 * setCurrentTime() will be called to update the internal state to the given time
 * Events passed to handleEvent occur at the time last passed to setCurrentTime.
 */
public interface TimedEventHandler<EventClass>
{
	public void setCurrentTime( long time ) throws Exception;
	public void handleEvent( EventClass evt ) throws Exception;
}
