package togos.networkrts.experimental.gameengine1.index;

public interface EntityUpdater<EC extends EntityRange>
{
	/**
	 * Return the entity to replace the given one with, or null to replace it with nothing.
	 * Additional entities may be added via the shell.
	 * It is generally more efficient to return the same entity if it is unchanged than
	 * to return null and push it to the shell.
	 */
	public EC update( EC e, EntityShell<EC> shell );
}