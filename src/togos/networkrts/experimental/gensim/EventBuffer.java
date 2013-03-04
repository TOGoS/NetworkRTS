package togos.networkrts.experimental.gensim;

public final class EventBuffer<EventClass> {
	long time;
	EventClass data;
	
	public EventBuffer( long initialTime ) {
		this.time = initialTime;
	}
	
	/**
	 * Convenience method for updating.
	 * Clamps the given newTime between the current time in the buffer
	 * (this is why it's important to initialize the buffer with a reasonable time value)
	 * and maxTime. 
	 */
	public final void update( EventClass data, long newTime, long maxTime ) {
		this.data = data;
		this.time = newTime < time ? time : newTime > maxTime ? maxTime : newTime;
	}
}
