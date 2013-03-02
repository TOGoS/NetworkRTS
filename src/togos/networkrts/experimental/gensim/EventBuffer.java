package togos.networkrts.experimental.gensim;

public final class EventBuffer<EventClass> {
	long time;
	EventClass data;
	
	public EventBuffer( long initialTime ) {
		this.time = initialTime;
	}
}