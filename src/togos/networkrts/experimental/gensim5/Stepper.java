package togos.networkrts.experimental.gensim5;

public interface Stepper<EventClass>
{
	public long getNextInternalUpdateTime();
	public void setCurrentTime(long time);
	public void handleEvent( EventClass evt );
}
