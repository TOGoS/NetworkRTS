package togos.networkrts.experimental.gensim;

import java.io.IOException;

/**
 * Objects implementing this may be in complete control of time passing.
 * They may use the system time or their own concept of time
 * to define currentTime and interpret returnBy, so long as
 * time never appears to go backwards.
 * 
 * ('real time' refers to that events can come in at any point,
 * not that the time values necessarily correspond to system time values)
 */
public interface RealTimeEventSource<EventClass>
{
	/**
	 * Wait for an event to appear before the specified time.
	 * - IF an event occurs, its code and/or data should be put in the buffer,
	 *   buf.timestamp set to the time at which the event occured, and true returned.
	 * - Otherwise, buf.timestamp should be set to returnBy and false returned.
	 * 
	 * After this returns buf.time must be between its old value and returnBy (inclusive). 
	 */
	public boolean recv( long returnBy, EventBuffer buf ) throws IOException, InterruptedException;
	/**
	 * Get the 'current time' as defined by this event source.
	 * Useful for initializing buf.time. 
	 */
	public long getCurrentTime();
}
