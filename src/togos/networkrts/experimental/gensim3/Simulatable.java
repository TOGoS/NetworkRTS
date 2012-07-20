package togos.networkrts.experimental.gensim3;

import java.util.Collection;

/**
 * A 'world' that can advance time in arbitrary increments, accept
 * externally-triggered events, and output events triggered internally in
 * response to requests to advance time and handle incoming events.
 * 
 * Input and output events may be entirely different types of things and their
 * base classes should be specified via the <I, O> template parameters.
 * 
 * Methods that return the new state of the world may return either the same
 * object after being modified or a new one representing the new state.
 */
public interface Simulatable<I, O> {
	/**
	 * Return the next timestamp that should be advancedTo provided no other
	 * external events occur.
	 */
	public long getNextInternalEventTimestamp();
	
	/**
	 * Advance the current time to the given timestamp. Any outgoing events that
	 * occur during up to and including at that timestamp will be added to
	 * outputEvents, and the new state will be returned.
	 */
	public Simulatable<I, O> advanceTo(long timestamp, Collection<O> outputEvents);
	
	/**
	 * Specify that an externally-triggered event occur at the current time. Any
	 * outgoing events that occur immediately as a result of this event will be
	 * added to outputEvents.
	 */
	public Simulatable<I, O> handle(I inputEvent, Collection<O> outputEvents);
}
