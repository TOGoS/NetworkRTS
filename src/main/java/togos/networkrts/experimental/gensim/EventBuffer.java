package togos.networkrts.experimental.gensim;

public final class EventBuffer<EventClass> {
	public long time;
	public EventClass payload;
	
	public EventBuffer( long initialTime ) {
		this.time = initialTime;
	}
	
	/**
	 * Convenience method for updating.
	 * Clamps the given newTime between the current time in the buffer
	 * (this is why it's important to initialize the buffer with a reasonable time value)
	 * and maxTime. 
	 */
	public final boolean update( EventClass data, long newTime, long maxTime ) {
		this.payload = data;
		this.time = newTime < time ? time : newTime > maxTime ? maxTime : newTime;
		return data != null;
	}
}
