package togos.networkrts.experimental.entree;

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
	
	public void eachEntity( ClipShape s, int requireFlags, int forbidFlags, EntityQuadTreeNode n, double nodeX, double nodeY, double nodeSize, Iterated<EntityClass> cb) {
		if( (n.entityFlags & requireFlags) != requireFlags ) return;
		// Can't filter out entire nodes by forbidden entity flags, since they are ORd together!
		
		double halfNodeSize = nodeSize / 2; // NODE PADDING
		double twiceNodeSize = nodeSize * 2;
		if( !s.intersectsRect(nodeX - halfNodeSize, nodeY - halfNodeSize, twiceNodeSize, twiceNodeSize ) ) {
			// NODE PADDING ^
			return;
		}
		
		for( int i=n.entities.length-1; i>=0; --i ) {
			EntityClass e = (EntityClass)n.entities[i];
			final int flags = e.getFlags();
			if( (flags & requireFlags) == requireFlags && (flags & forbidFlags) == 0 ) {
				cb.item( e );
			}
		}
		
		eachEntity( s, requireFlags, forbidFlags, n.n0, nodeX             , nodeY             , halfNodeSize, cb );
		eachEntity( s, requireFlags, forbidFlags, n.n1, nodeX+halfNodeSize, nodeY             , halfNodeSize, cb );
		eachEntity( s, requireFlags, forbidFlags, n.n2, nodeX             , nodeY+halfNodeSize, halfNodeSize, cb );
		eachEntity( s, requireFlags, forbidFlags, n.n3, nodeX+halfNodeSize, nodeY+halfNodeSize, halfNodeSize, cb );
	}
	
	public void eachEntity(	ClipShape s, int requireFlags, int forbidFlags, Iterated<EntityClass> cb) {
		eachEntity( s, requireFlags | PlaneEntity.FLAG_EXISTS, forbidFlags, root, 0, 0, rootSize, cb );
	}
}
