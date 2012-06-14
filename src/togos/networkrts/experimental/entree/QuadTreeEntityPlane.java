package togos.networkrts.experimental.entree;

import togos.networkrts.experimental.cshape.CShape;

public class QuadTreeEntityPlane<EntityClass extends PlaneEntity> implements EntityPlane<EntityClass>
{
	public final double sizeX, sizeY;
	public final EntityQuadTreeNode root;
	public final double rootSize;
	
	public QuadTreeEntityPlane( double sizeX, double sizeY, EntityQuadTreeNode root, double rootSize ) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.root = root;
		this.rootSize = rootSize;
	}
	
	public EntityPlane update(EntityPlaneUpdate u) {
		return new QuadTreeEntityPlane( sizeX, sizeY, root.update(u, 0, 0, rootSize), rootSize );
	}
	
	public void eachEntity(	CShape s, int requireFlags, int forbidFlags, Iterated<EntityClass> cb) {
		root.eachEntity( s, requireFlags | PlaneEntity.FLAG_EXISTS, forbidFlags, 0, 0, rootSize, cb );
	}
}
