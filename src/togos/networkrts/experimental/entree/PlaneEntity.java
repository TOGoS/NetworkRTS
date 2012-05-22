package togos.networkrts.experimental.entree;

public interface PlaneEntity
{
	public static final int FLAG_EXISTS = 0x1;
	
	/**
	 * Entities may have unique, persistent IDs that remain constant even as
	 * the entity's state changes.  Persistent IDs are only needed to
	 * manipulate individual entities directly.  Entities for which this
	 * capability is not needed may return themselves as their ID.
	 */
	public Object getEntityId();
	
	// These 3 properties define the entity's position in the universe:
	
	public Object getPlaneId();
	public double getX();
	public double getY();
	
	/** Maximum physical or visual radius; the entity may be completely
	 * ignored by events that occur outside of this */
	public double getMaxRadius();
	
	/** Arbitrarily-defined flags except for flag 1, which means that the
	 * entity exists (all entities should have this set) */
	public int getFlags();
}
