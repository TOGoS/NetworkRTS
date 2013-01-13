package togos.networkrts.experimental.gensim5;

public interface RealTimeEventSource<EventClass>
{
	public EventClass recv( long returnBy );
	public long getCurrentTime();
}