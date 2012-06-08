package togos.networkrts.experimental.gensim;

public interface Timekeeper
{
	public long getCurrentTimestamp();
	public void waitUntil( long timestamp ) throws InterruptedException;
}
