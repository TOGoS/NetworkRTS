package togos.networkrts.experimental.gensim5;

public interface Simulation<EventClass>
{
	public long getNextInternalUpdateTime();
	public void setCurrentTime(long time);
	public void handleEvent( EventClass evt );
}