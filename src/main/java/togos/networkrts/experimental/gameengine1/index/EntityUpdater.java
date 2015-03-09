package togos.networkrts.experimental.gameengine1.index;

import java.util.Collection;

public interface EntityUpdater<EC extends EntityRange>
{
	/**
	 * Return the entity to replace the given one with, or null to replace it with nothing.
	 * 
	 * An entity may add additional entities to the world by adding them to the newEntities collection.
	 */
	public EC update( EC e, Collection<EC> newEntities );
}