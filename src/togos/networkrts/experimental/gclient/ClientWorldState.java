package togos.networkrts.experimental.gclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds information that a client knows about the world.
 * 
 * The server-client protocol looks something like:
 * 
 *   clear-cache
 *   clear-state
 *   set-cache <name> <object-spec>
 *   set-state <name> <object-spec>
 *   event <object-spec>
 *   timestamp <timestamp>
 * 
 * Where object-spec may contain references to cache objects set earlier.
 */ 
public class ClientWorldState
{
	/*
	 * Both cacheObjects and stateObjects are used by servers to
	 * store persistent objects.
	 * 
	 * Updates may be made in terms of cache objects to avoid sending
	 * entire structures where substructures would be unmodified.
	 * Replacing a named object should *not* affect structures that
	 * were created referencing that name earlier; i.e. names should
	 * be dereferenced when messages are interpreted, and messages
	 * should be interpreted in the order they arrive.
	 * 
	 * Both maps are managed by the server; it can clear the list and
	 * set items.
	 * 
	 * If a cache object is missing when referenced, the client may
	 * suspend interpretation and re-request the missing object.
	 */
	
	/**
	 * Map of objects whose only purpose it to be used when defining
	 * other structures.
	 */
	Map cacheObjects = new HashMap();
	
	/**
	 * Objects representing the state of the world.
	 */
	Map stateObjects = new HashMap();
	
	/**
	 * List of 'event' objects that have occurred recently.
	 * The client is responsible for cleaning out expired events.
	 */
	Collection events = new ArrayList();
	
	long currentTime;
}
