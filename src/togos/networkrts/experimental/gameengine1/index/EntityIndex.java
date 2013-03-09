package togos.networkrts.experimental.gameengine1.index;

public interface EntityIndex<EC extends Entity>
{
	public EntityIndex<EC> with(EC e);
	public void forEachEntityIntersecting(AABB bounds, Visitor<EC> callback);
	public EntityIndex<EC> updateEntities( long requireFlags, EntityUpdater<EC> u );
	/** Get the flags of all entities in the system ORed together. */
	public long getAllEntityFlags();
	
	// TODO: I expect these would be useful:
	//public EntityIndex<EC> updateEntities( Collection<EC> entities, EntityUpdater<EC> u );
	//public EC getEntityByTag( Object tag );
}
