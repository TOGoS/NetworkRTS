package togos.networkrts.experimental.entree;

import togos.networkrts.experimental.shape.RectIntersector;

public interface EntityPlane<EntityClass extends PlaneEntity>
{
	public EntityPlane<EntityClass> update( EntityPlaneUpdate u );
	public void eachEntity( RectIntersector s, int requireFlags, int forbidFlags, Iterated<EntityClass> cb );
}
