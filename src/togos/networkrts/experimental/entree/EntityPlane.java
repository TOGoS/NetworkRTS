package togos.networkrts.experimental.entree;

import togos.networkrts.experimental.cshape.CShape;

public interface EntityPlane<EntityClass extends PlaneEntity>
{
	public EntityPlane<EntityClass> update( EntityPlaneUpdate u );
	public void eachEntity( CShape s, int requireFlags, int forbidFlags, Iterated<EntityClass> cb );
}
