package togos.networkrts.experimental.entree;

public class EntityQuadTreeNode
{
	public static final EntityQuadTreeNode EMPTY = new EntityQuadTreeNode();
	
	public final PlaneEntity[] entities;
	/**
	 * Subnodes which together fill this node.
	 * 0 1
	 * 2 3
	 */
	public final EntityQuadTreeNode n0, n1, n2, n3;
	public final int entityFlags;
	
	private EntityQuadTreeNode() {
		this.entities = new PlaneEntity[0];
		this.n0 = this;
		this.n1 = this;
		this.n2 = this;
		this.n3 = this;
		this.entityFlags = 0;
	}
	
	public EntityQuadTreeNode( PlaneEntity[] e,
		EntityQuadTreeNode n0, EntityQuadTreeNode n1, EntityQuadTreeNode n2, EntityQuadTreeNode n3
	) {
		this.entities = e;
		this.n0 = n0;
		this.n1 = n1;
		this.n2 = n2;
		this.n3 = n3;
		
		int eflags = n0.entityFlags | n1.entityFlags | n2.entityFlags | n3.entityFlags;
		for( int i=0; i<entities.length; ++i ) {
			eflags |= entities[i].getFlags();
		}
		this.entityFlags = eflags;
	}
	
	public static final double SQRT2 = Math.sqrt(2); 
	
	protected static final boolean fits( PlaneEntity pe, double nodeX, double nodeY, double nodeSize ) {
		final double x = pe.getX();
		if( x < nodeX || x >= nodeX+nodeSize ) return false;
		final double y = pe.getY();
		if( y < nodeY || y >= nodeY+nodeSize ) return false;
		
		// Whether or not an entity whose center is inside a node
		// can be placed within that node is arbitrarily defined as
		// fitting within the area consisting of that node + padding
		// on every side (which is 50% the width of the node).
		final double nodePadding = nodeSize * 0.5; // NODE PADDING
		final double rad = pe.getMaxRadius();
		
		if( x - rad < nodeX - nodePadding || x + rad > nodeX+nodeSize + nodePadding ) return false;
		if( y - rad < nodeY - nodePadding || y + rad > nodeY+nodeSize + nodePadding ) return false;
		
		return true;
	}

	public EntityQuadTreeNode update( EntityPlaneUpdate u, double myX, double myY, double mySize ) {
		final double halfMySize = mySize/2;
		
		boolean anyModifications = false;
		
		int removedEntityCount = 0;
		
		for( int r=0; r<u.removeCount; ++r ) {
			if( fits(u.remove[r], myX, myY, mySize) ) {
				final Object removeEntityId = u.remove[r].getEntityId(); 
				for( int e=0; e<entities.length; ++e ) {
					if( removeEntityId.equals(entities[e].getEntityId()) ) ++removedEntityCount;
				}
				anyModifications = removedEntityCount > 0;
			}
		}
		
		PlaneEntity[] newEntities = null;
		int newEntityCount = 0;
		
		for( int a=0; a<u.addCount; ++a ) {
			if( fits(u.add[a], myX, myY, mySize) ) {
				anyModifications = true;
				if(
					!fits(u.add[a], myX           , myY           , halfMySize) &&
					!fits(u.add[a], myX+halfMySize, myY           , halfMySize) &&
					!fits(u.add[a], myX           , myY+halfMySize, halfMySize) &&
					!fits(u.add[a], myX+halfMySize, myY+halfMySize, halfMySize)
				) {
					if( newEntities == null ) {
						newEntities = new PlaneEntity[entities.length + u.addCount - removedEntityCount];
					}
					newEntities[newEntityCount++] = u.add[a];
				}
			}
		}
		
		if( !anyModifications ) return this;
		
		if( newEntities == null && removedEntityCount == 0 ) {
			newEntities = entities;
		} else {
			if( newEntities == null ) newEntities = new PlaneEntity[entities.length - removedEntityCount];
			
			for( int e=0; e<entities.length; ++e ) {
				boolean removed = false;
				for( int r=0; r<u.removeCount; ++r ) {
					final Object removeEntityId = u.remove[r].getEntityId(); 
					if( removeEntityId.equals(entities[e].getEntityId()) ) removed = true;
				}
				if( !removed ) {
					newEntities[newEntityCount++] = entities[e];
				}
			}
			
			if( newEntities.length > newEntityCount ) {
				PlaneEntity[] newEntities2 = new PlaneEntity[newEntityCount];
				for( int i=newEntityCount-1; i>=0; --i ) {
					newEntities2[i] = newEntities[i];
				}
				newEntities = newEntities2;
			}
		}
		
		return new EntityQuadTreeNode(
			newEntities,
			n0.update(u, myX           , myY           , halfMySize),
			n1.update(u, myX+halfMySize, myY           , halfMySize),
			n2.update(u, myX           , myY+halfMySize, halfMySize),
			n3.update(u, myX+halfMySize, myY+halfMySize, halfMySize)
		);
	}
	
	public void eachEntity( ClipShape s, int requireFlags, int forbidFlags, double nodeX, double nodeY, double nodeSize, Iterated cb) {
		if( (entityFlags & requireFlags) != requireFlags ) return;
		// Can't filter out entire nodes by forbidden entity flags, since they are ORd together!
		
		double halfNodeSize = nodeSize / 2; // NODE PADDING
		double twiceNodeSize = nodeSize * 2;
		if( !s.intersectsRect(nodeX - halfNodeSize, nodeY - halfNodeSize, twiceNodeSize, twiceNodeSize ) ) {
			// NODE PADDING ^
			return;
		}
		
		for( int i=entities.length-1; i>=0; --i ) {
			PlaneEntity e = (PlaneEntity)entities[i];
			final int flags = e.getFlags();
			if( (flags & requireFlags) == requireFlags && (flags & forbidFlags) == 0 ) {
				cb.item( e );
			}
		}
		
		n0.eachEntity( s, requireFlags, forbidFlags, nodeX             , nodeY             , halfNodeSize, cb );
		n1.eachEntity( s, requireFlags, forbidFlags, nodeX+halfNodeSize, nodeY             , halfNodeSize, cb );
		n2.eachEntity( s, requireFlags, forbidFlags, nodeX             , nodeY+halfNodeSize, halfNodeSize, cb );
		n3.eachEntity( s, requireFlags, forbidFlags, nodeX+halfNodeSize, nodeY+halfNodeSize, halfNodeSize, cb );
	}
}
