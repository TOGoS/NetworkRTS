package togos.networkrts.experimental.gameengine1.index;

import java.util.ArrayList;

import togos.networkrts.util.BitAddressUtil;

public class EntitySpatialTreeIndex<EC extends EntityRange> implements EntityIndex<EC>
{
	static class EntityTreeNode<EC extends EntityRange> extends AABB implements EntityRange {
		private ArrayList<EC> localEntities = new ArrayList<EC>();
		private EntityTreeNode<EC> subNodeA = null;
		private EntityTreeNode<EC> subNodeB = null;
		protected long minBitAddress = BitAddressUtil.MAX_ADDRESS;
		protected long maxBitAddress = BitAddressUtil.MIN_ADDRESS;
		protected long nextAutoUpdateTime = Long.MAX_VALUE;
		protected int totalEntityCount = 0;
		
		public EntityTreeNode( double minX, double minY, double minZ, double maxX, double maxY, double maxZ, EntityTreeNode<EC> subNodeA, EntityTreeNode<EC> subNodeB ) {
			super( minX, minY, minZ, maxX, maxY, maxZ );
			this.subNodeA = subNodeA;
			this.subNodeB = subNodeB;
			if( hasSubNodes() ) {
				this.totalEntityCount = subNodeA.totalEntityCount + subNodeB.totalEntityCount;
				this.minBitAddress = subNodeA.minBitAddress & subNodeB.minBitAddress;
				this.maxBitAddress = subNodeA.maxBitAddress & subNodeB.maxBitAddress;
				this.nextAutoUpdateTime = Math.min(subNodeA.getNextAutoUpdateTime(), subNodeB.getNextAutoUpdateTime());
			}
		}
		public EntityTreeNode( double minX, double minY, double minZ, double maxX, double maxY, double maxZ ) {
			this( minX, minY, minZ, maxX, maxY, maxZ, null, null );
		}
		public EntityTreeNode(AABB copyOf) {
			super(copyOf);
		}
		public EntityTreeNode() {
			this(
				Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY
			);
		}
		
		private EntityTreeNode<EC> subDivide() {
			ArrayList<EC> oldEntities = localEntities;
			double
				bMinX = maxX, bMinY = maxY, bMinZ = maxZ,
				bMaxX = minX, bMaxY = minY, bMaxZ = minZ;
			for( EntityRange e : oldEntities ) {
				AABB aabb = e.getAABB();
				assert aabb.isFinite();
				bMinX = Math.min(bMinX, aabb.minX);
				bMinY = Math.min(bMinY, aabb.minY);
				bMinZ = Math.min(bMinZ, aabb.minZ);
				bMaxX = Math.max(bMaxX, aabb.maxX);
				bMaxY = Math.max(bMaxY, aabb.maxY);
				bMaxZ = Math.max(bMaxZ, aabb.maxZ);
			}
			
			char maxDim = 'x';
			if( bMaxY-bMinY > bMaxX-bMinX ) maxDim = 'y';
			if( bMaxZ-bMinZ > bMaxY-bMinY ) maxDim = 'z';
			
			switch( maxDim ) {
			case 'x':
				double divX = bMinX+(bMaxX-bMinX)/2;
				subNodeA = new EntityTreeNode<EC>( minX, minY, minZ, divX, maxY, maxZ );
				subNodeB = new EntityTreeNode<EC>( divX, minY, minZ, maxX, maxY, maxZ );
				break;
			case 'y':
				double divY = bMinY+(bMaxY-bMinY)/2;
				subNodeA = new EntityTreeNode<EC>( minX, minY, minZ, maxX, divY, maxZ );
				subNodeB = new EntityTreeNode<EC>( minX, divY, minZ, maxX, maxY, maxZ );
				break;
			case 'z':
				double divZ = bMinZ+(bMaxZ-bMinZ)/2;
				subNodeA = new EntityTreeNode<EC>( minX, minY, minZ, maxX, maxY, divZ );
				subNodeB = new EntityTreeNode<EC>( minX, minY, divZ, maxX, maxY, maxZ );
				break;
			}
			
			localEntities = new ArrayList<EC>();
			for( EC e : oldEntities ) {
				AABB aabb = e.getAABB();
				if( subNodeA.contains(aabb) ) {
					subNodeA.add(e);
				} else if( subNodeB.contains(aabb) ) {
					subNodeB.add(e);
				} else {
					localEntities.add(e);
				}
			}
			subNodeA.freeze();
			subNodeB.freeze();
			return this;
		}
		
		protected boolean hasSubNodes() {
			return subNodeA != null;
		}
		
		private boolean frozen;
		protected EntityTreeNode<EC> freeze() {
			if( frozen ) return this;
			
			if( hasSubNodes() ) {
				subNodeA = subNodeA.freeze();
				subNodeB = subNodeB.freeze();
			} else if( localEntities.size() > 16 ) {
				subDivide();
			}
			this.frozen = true;
			return this;
		}
		protected EntityTreeNode<EC> thaw() {
			if( !frozen ) return this;
			
			EntityTreeNode<EC> thawed = new EntityTreeNode<EC>();
			thawed.subNodeA = subNodeA;
			thawed.subNodeB = subNodeB;
			thawed.localEntities = new ArrayList<EC>(localEntities);
			thawed.minBitAddress = minBitAddress;
			thawed.maxBitAddress = maxBitAddress;
			thawed.nextAutoUpdateTime = nextAutoUpdateTime;
			thawed.totalEntityCount = totalEntityCount;
			return thawed;
		}
		
		// Note: This subdivision algorithm sucks.
		public void add(EC e) {
			assert !frozen;
			AABB aabb = e.getAABB();
			assert contains(aabb);
			
			minBitAddress &= e.getMinBitAddress();
			maxBitAddress &= e.getMaxBitAddress();
			nextAutoUpdateTime = Math.min( nextAutoUpdateTime, e.getNextAutoUpdateTime() );
			totalEntityCount += 1;
			
			if( hasSubNodes() ) {
				// Already subdivided
				if( subNodeA.contains(aabb) ) {
					subNodeA = subNodeA.with(e);
				} else if( subNodeB.contains(aabb) ) {
					subNodeB = subNodeB.with(e);
				} else {
					localEntities.add(e);
				}
			} else {
				localEntities.add(e);
			}
		}
		
		public EntityTreeNode<EC> with(EC e) {
			AABB aabb = e.getAABB();
			assert contains(aabb);
			EntityTreeNode<EC> n = thaw();
			n.add(e);
			return n;
		}
		
		public void forEachEntity( EntityRange er, Visitor<EC> callback ) {
			if( !EntityRanges.intersects(er, this) ) return;
			
			for( EC e : localEntities ) {
				if( EntityRanges.intersects(er, e) ) callback.visit(e);
			}
			if( hasSubNodes() ) {
				subNodeA.forEachEntity( er, callback );
				subNodeB.forEachEntity( er, callback );
			}
		}
		
		public void forEachEntityIntersecting( AABB bounds, Visitor<EC> callback ) {
			if( !intersects(bounds) ) return;
			
			for( EC e : localEntities ) {
				if( e.getAABB().intersects(bounds) ) callback.visit(e);
			}
			if( hasSubNodes() ) {
				subNodeA.forEachEntityIntersecting(bounds, callback);
				subNodeB.forEachEntityIntersecting(bounds, callback);
			}
		}
		
		/**
		 * Updates this entity tree node in-place.
		 * 
		 * Shell must NOT modify the entity tree immediately.
		 * It should push new entities into a list for
		 * re-adding after update of the entire tree has returned.
		 * 
		 * Returns null if the entire node would be updated.
		 */
		public EntityTreeNode<EC> update( EntityRange er, EntityUpdater<EC> u, EntityShell<EC> shell ) {
			if( !EntityRanges.intersects(er, this) ) return this;
			
			for( EC e : localEntities ) {
				EC updated = EntityRanges.intersects(er, e) ? u.update(e, shell) : e;
				if( updated != null ) shell.add( updated );
			}
			
			if( !hasSubNodes() ) return null;
			
			EntityTreeNode<EC> newNodeA = subNodeA.update(er, u, shell);
			EntityTreeNode<EC> newNodeB = subNodeB.update(er, u, shell);
			
			if( newNodeA == null && newNodeB == null ) return null;
			
			if( newNodeA == null ) newNodeA = new EntityTreeNode<EC>((AABB)subNodeA); 
			if( newNodeB == null ) newNodeB = new EntityTreeNode<EC>((AABB)subNodeB);
			
			return new EntityTreeNode<EC>(
				minX, minY, minZ,
				maxX, maxY, maxZ,
				newNodeA, newNodeB
			);
		}
		@Override public long getMinBitAddress() { return minBitAddress; }
		@Override public long getMaxBitAddress() { return maxBitAddress; }
		@Override public AABB getAABB() { return this; }
		@Override public long getNextAutoUpdateTime() { return nextAutoUpdateTime; }
	}
	
	final EntityTreeNode<EC> entityTree;
	
	protected EntitySpatialTreeIndex( EntityTreeNode<EC> tree ) {
		this.entityTree = tree;
	}
	public EntitySpatialTreeIndex() {
		this(new EntityTreeNode<EC>());
	}
	
	protected EntitySpatialTreeIndex<EC> thaw() {
		return new EntitySpatialTreeIndex<EC>(entityTree.thaw());
	}
	
	public void add(EC e) {
		entityTree.add(e);
	}
	
	@Override
	public EntitySpatialTreeIndex<EC> with(EC e) {
		EntitySpatialTreeIndex<EC> newIndex = thaw();
		newIndex.add(e);
		return newIndex;
	}
	
	@Override
	public void forEachEntity(EntityRange er, Visitor<EC> callback) {
		entityTree.forEachEntity(er, callback);
	}
	
	/*
	public EntityIndex updateEntities( final EntityUpdater u ) {
		final EntityIndex newIndex = new EntityIndex();
		final EntityShell shell = new EntityShell() {
			@Override public void add( Entity e ) {
				newIndex.add(e);
			}
		};
		entityTree.forEachEntity( Entity.FLAG_EXISTING, new Visitor<Entity>() {
			@Override public void visit(Entity e) {
				if( (e = u.update(e, shell)) != null ) {
					newIndex.add(e);
				}
			}
		});
		return newIndex;
	}
	*/
	
	@Override
	public EntitySpatialTreeIndex<EC> updateEntities( EntityRange er, final EntityUpdater<EC> u ) {
		final ArrayList<EC> newEntityList = new ArrayList<EC>();
		newEntityList.clear();
		EntityShell<EC> shell = new EntityShell<EC>() {
			@Override public void add(EC e) {
				newEntityList.add(e);
			}
		};
		EntityTreeNode<EC> newEntityTree = entityTree.update(er, u, shell);
		if( newEntityTree == null ) newEntityTree = new EntityTreeNode<EC>();
		for( EC e : newEntityList ) {
			newEntityTree.add(e);
		}
		return new EntitySpatialTreeIndex<EC>(newEntityTree.freeze());
	}
	
	public int getTotalEntityCount() { return entityTree.totalEntityCount; }
	@Override public AABB getAABB() { return entityTree.getAABB(); }
	@Override public long getMinBitAddress() { return entityTree.getMinBitAddress(); }
	@Override public long getMaxBitAddress() { return entityTree.getMaxBitAddress(); }
	@Override public long getNextAutoUpdateTime() { return entityTree.getNextAutoUpdateTime(); }
}
