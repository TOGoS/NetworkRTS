package togos.networkrts.experimental.gameengine1.index;

public interface EntityIndex<EC extends EntityRange> extends EntityRange
{
	public EntityIndex<EC> with(EC e);
	public void forEachEntity(EntityRange er, Visitor<EC> callback);
	public EntityIndex<EC> updateEntities(EntityRange er, EntityUpdater<EC> u );
	
	// TODO: I expect these would be useful:
	//public EntityIndex<EC> updateEntities( Collection<EC> entities, EntityUpdater<EC> u );
}
