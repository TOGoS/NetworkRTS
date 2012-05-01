package togos.networkrts.experimental.entree;

public interface EntityPlane<EntityClass extends PlaneEntity>
{
	public EntityPlane<EntityClass> update( EntityPlaneUpdate u );
	public void eachEntity( ClipShape s, int requireFlags, int forbidFlags, Iterated<EntityClass> cb );
}
