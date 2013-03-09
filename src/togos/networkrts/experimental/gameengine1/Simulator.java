package togos.networkrts.experimental.gameengine1;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;

import togos.networkrts.experimental.gensim.BaseMutableAutoUpdatable;
import togos.networkrts.experimental.gensim.EventLoop;
import togos.networkrts.experimental.gensim.QueuelessRealTimeEventSource;

public class Simulator extends BaseMutableAutoUpdatable<Object>
{
	static class Entity
	{
		public final Object tag;
		public final long time;
		public final double x, y, z;
		public final double vx, vy, vz;
		public final double ax, ay, az;
		public final double radius;
		public final double mass;
		public final Color color;
		public final EntityBehavior behavior;
		
		public final AABB boundingBox;
		public final long flags;
		
		public Entity(
			Object tag, long time,
			double x, double y, double z,
			double vx, double vy, double vz,
			double ax, double ay, double az,
			double radius,	double mass, Color color,
			long flags, EntityBehavior behavior
		) {
			this.tag = tag;
			this.time = time;
			this.x = x; this.y = y; this.z = z;
			this.vx = vx; this.vy = vy; this.vz = vz;
			this.ax = ax; this.ay = ay; this.az = az;
			this.radius = radius;
			this.mass = mass; this.color = color;
			this.behavior = behavior;
			this.boundingBox = new AABB(
				x-radius, y-radius, z-radius,
				x+radius, y+radius, z+radius
			);
			this.flags = flags;
		}
		
		public Entity withUpdatedPosition( long targetTime ) {
			final double duration = (targetTime-time)/1000.0;
			final double halfDurationSquared = duration*duration/2;
			return withPosition(
				targetTime,
				x + vx*duration + ax*halfDurationSquared,
				y + vy*duration + ay*halfDurationSquared,
				z + vz*duration + az*halfDurationSquared,
				vx + ax*duration,
				vy + ay*duration,
				vz + az*duration,
				ax, ay, az
			);
		}
		
		public Entity withPosition( long time,
			double x, double y, double z,
			double vx, double vy, double vz,
			double ax, double ay, double az
		) {
			return new Entity(
				tag, time,
				x, y, z, vx, vy, vz, ax, ay, az,
				radius, mass, color, flags, behavior
			);
		}
	}
	
	interface EntityShell
	{
		public void add( Entity e );
	}
	
	interface EntityUpdater
	{
		/**
		 * Return the entity to replace the given one with, or null to replace it with nothing.
		 * Additional entities may be added via the shell.
		 * It is generally more efficient to return the same entity if it is unchanged than
		 * to return null and push it to the shell.
		 */
		public Entity update( Entity e, EntityShell shell );
	}
	
	interface EntityBehavior
	{
		public Entity onMove( long time, Entity self, EntityShell shell );
		public Entity onCollision( long time, Entity self, Entity other, EntityShell shell );
	}
	
	static final EntityBehavior NULL_BEHAVIOR = new EntityBehavior() {
		@Override public Entity onMove( long time, Entity self, EntityShell shell ) { return self; }
		@Override public Entity onCollision( long time, Entity self, Entity other, EntityShell shell ) { return self; }
	};
	
	/** Axis-aligned bounding box */
	static class AABB {
		final double minX, minY, minZ, maxX, maxY, maxZ;
		
		public AABB( double x0, double y0, double z0, double x1, double y1, double z1 ) {
			assert x0 < x1;
			assert y0 < y1;
			assert z0 < z1;
			minX = x0;  maxX = x1;
			minY = y0;  maxY = y1;
			minZ = z0;  maxZ = z1;
		}
		
		public AABB(AABB o) {
			this( o.minX, o.minY, o.minZ, o.maxX, o.maxY, o.maxZ );
		}
		
		public final AABB centered( double x, double y, double z, double rad ) {
			return new AABB( x-rad, y-rad, z-rad, x+rad, y+rad, z+rad );
		}
		
		public final boolean contains( AABB other ) {
			if( other.minX < minX ) return false;
			if( other.minY < minY ) return false;
			if( other.minZ < minZ ) return false;
			if( other.maxX > maxX ) return false;
			if( other.maxY > maxY ) return false;
			if( other.maxZ > maxZ ) return false;
			return true;
		}
		
		public final boolean intersects( AABB other ) {
			if( maxX < other.minX ) return false;
			if( maxY < other.minY ) return false;
			if( maxZ < other.minZ ) return false;
			if( minX > other.maxX ) return false;
			if( minY > other.maxY ) return false;
			if( minZ > other.maxZ ) return false;
			return true;
		}
		
		protected static boolean isFinite( double v ) {
			return !Double.isNaN(v) && !Double.isInfinite(v);
		}
		
		public boolean isFinite() {
			return
				isFinite(minX) && isFinite(minY) && isFinite(minZ) &&
				isFinite(maxX) && isFinite(maxY) && isFinite(maxZ);
		}
	}
	
	interface Visitor<Type> {
		public void visit( Type v );
	}
	
	protected static final boolean solidCollision( Entity e1, Entity e2 ) {
		assert e1 != e2;
		
		double dx = e1.x-e2.x, dy = e1.y-e2.y, dz = e1.z-e2.z;
		double distSquared = dx*dx + dy*dy + dz*dz;
		double radSum = e1.radius + e2.radius;
		return distSquared < radSum*radSum;
	}
	
	static class EntityIndex
	{
		static class EntityTreeNode extends AABB {
			private ArrayList<Entity> localEntities = new ArrayList<Entity>();
			private EntityTreeNode subNodeA = null;
			private EntityTreeNode subNodeB = null;
			protected long flags = 0; // Flags of all entities within, including subnodes
			protected long totalEntityCount = 0;
			
			public EntityTreeNode( double minX, double minY, double minZ, double maxX, double maxY, double maxZ, EntityTreeNode subNodeA, EntityTreeNode subNodeB ) {
				super( minX, minY, minZ, maxX, maxY, maxZ );
				this.subNodeA = subNodeA;
				this.subNodeB = subNodeB;
				if( hasSubNodes() ) {
					this.totalEntityCount = subNodeA.totalEntityCount + subNodeB.totalEntityCount;
					this.flags = subNodeA.flags | subNodeB.flags;
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
			
			private EntityTreeNode subDivide() {
				//System.err.println("Subdividing "+minX+","+minY+","+minZ+" "+maxX+","+maxY+","+maxZ);
				ArrayList<Entity> oldEntities = localEntities;
				double
					bMinX = maxX, bMinY = maxY, bMinZ = maxZ,
					bMaxX = minX, bMaxY = minY, bMaxZ = minZ;
				for( Entity e : oldEntities ) {
					assert e.boundingBox.isFinite();
					bMinX = Math.min(bMinX, e.boundingBox.minX);
					bMinY = Math.min(bMinY, e.boundingBox.minY);
					bMinZ = Math.min(bMinZ, e.boundingBox.minZ);
					bMaxX = Math.max(bMaxX, e.boundingBox.maxX);
					bMaxY = Math.max(bMaxY, e.boundingBox.maxY);
					bMaxZ = Math.max(bMaxZ, e.boundingBox.maxZ);
				}
				
				char maxDim = 'x';
				if( bMaxY-bMinY > bMaxX-bMinX ) maxDim = 'y';
				if( bMaxZ-bMinZ > bMaxY-bMinY ) maxDim = 'z';
				
				switch( maxDim ) {
				case 'x':
					double divX = bMinX+(bMaxX-bMinX)/2;
					subNodeA = new EntityTreeNode( minX, minY, minZ, divX, maxY, maxZ );
					subNodeB = new EntityTreeNode( divX, minY, minZ, maxX, maxY, maxZ );
					break;
				case 'y':
					double divY = bMinY+(bMaxY-bMinY)/2;
					subNodeA = new EntityTreeNode( minX, minY, minZ, maxX, divY, maxZ );
					subNodeB = new EntityTreeNode( minX, divY, minZ, maxX, maxY, maxZ );
					break;
				case 'z':
					double divZ = bMinZ+(bMaxZ-bMinZ)/2;
					subNodeA = new EntityTreeNode( minX, minY, minZ, maxX, maxY, divZ );
					subNodeB = new EntityTreeNode( minX, minY, divZ, maxX, maxY, maxZ );
					break;
				}
				
				localEntities = new ArrayList<Entity>();
				for( Entity e : oldEntities ) {
					if( subNodeA.contains(e.boundingBox) ) {
						subNodeA.add(e);
					} else if( subNodeB.contains(e.boundingBox) ) {
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
			protected EntityTreeNode freeze() {
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
			protected EntityTreeNode thaw() {
				if( !frozen ) return this;
				
				EntityTreeNode thawed = new EntityTreeNode();
				thawed.subNodeA = subNodeA;
				thawed.subNodeB = subNodeB;
				thawed.localEntities = new ArrayList<Entity>(localEntities);
				thawed.flags = flags;
				thawed.totalEntityCount = totalEntityCount;
				return thawed;
			}
			
			// Note: This subdivision algorithm sucks.
			public void add(Entity e) {
				assert !frozen;
				assert contains(e.boundingBox);
				
				flags |= e.flags;
				totalEntityCount += 1;
				
				if( hasSubNodes() ) {
					// Already subdivided
					if( subNodeA.contains(e.boundingBox) ) {
						subNodeA = subNodeA.with(e);
					} else if( subNodeB.contains(e.boundingBox) ) {
						subNodeB = subNodeB.with(e);
					} else {
						localEntities.add(e);
					}
				} else {
					localEntities.add(e);
				}
			}
			
			public EntityTreeNode with(Entity e) {
				assert contains(e.boundingBox);
				EntityTreeNode n = thaw();
				n.add(e);
				return n;
			}
			
			public void forEachEntity( long requireFlags, Visitor<Entity> callback ) {
				if( (flags & requireFlags) != requireFlags ) return;
				
				for( Entity e : localEntities ) {
					if( (e.flags & requireFlags) == requireFlags ) callback.visit(e);
				}
				if( hasSubNodes() ) {
					subNodeA.forEachEntity( requireFlags, callback );
					subNodeB.forEachEntity( requireFlags, callback );
				}
			}
			
			public void forEachEntityIntersecting( AABB bounds, Visitor<Entity> callback ) {
				if( !intersects(bounds) ) return;
				
				for( Entity e : localEntities ) {
					if( e.boundingBox.intersects(bounds) ) callback.visit(e);
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
			public EntityTreeNode update( long requireFlags, EntityUpdater u, EntityShell shell ) {
				if( (flags & requireFlags) != requireFlags ) return this;
				
				for( Entity e : localEntities ) {
					Entity updated = (e.flags & requireFlags) == requireFlags ? u.update(e, shell) : e;
					if( updated != null ) shell.add( updated );
				}
				
				if( !hasSubNodes() ) return null;
				
				EntityTreeNode newNodeA = subNodeA.update(requireFlags, u, shell);
				EntityTreeNode newNodeB = subNodeB.update(requireFlags, u, shell);
				
				if( newNodeA == null && newNodeB == null ) return null;
				
				if( newNodeA == null ) newNodeA = new EntityTreeNode((AABB)subNodeA); 
				if( newNodeB == null ) newNodeB = new EntityTreeNode((AABB)subNodeB);
				
				return new EntityTreeNode(
					minX, minY, minZ,
					maxX, maxY, maxZ,
					newNodeA, newNodeB
				);
			}
		}
		
		final EntityTreeNode entityTree;
		
		protected EntityIndex( EntityTreeNode tree ) {
			this.entityTree = tree;
		}
		public EntityIndex() {
			this(new EntityTreeNode());
		}
		
		protected EntityIndex thaw() {
			return new EntityIndex(entityTree.thaw());
		}
		
		public void add(Entity e) {
			entityTree.add(e);
		}
		
		public EntityIndex with(Entity e) {
			EntityIndex newIndex = thaw();
			newIndex.add(e);
			return newIndex;
		}
		
		public void forEachEntityIntersecting(AABB bounds, Visitor<Entity> callback) {
			entityTree.forEachEntityIntersecting(bounds, callback);
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
		
		public EntityIndex updateEntities( long requireFlags, final EntityUpdater u ) {
			final ArrayList<Entity> newEntityList = new ArrayList<Entity>();
			newEntityList.clear();
			EntityShell shell = new EntityShell() {
				@Override public void add(Entity e) {
					newEntityList.add(e);
				}
			};
			EntityTreeNode newEntityTree = entityTree.update(requireFlags, u, shell);
			if( newEntityTree == null ) newEntityTree = new EntityTreeNode();
			for( Entity e : newEntityList ) {
				newEntityTree.add(e);
			}
			return new EntityIndex(newEntityTree.freeze());
		}
	}
	
	EntityIndex entityIndex = new EntityIndex();
	public long physicsInterval = 10;
	HashSet<Runnable> worldUpdateListeners = new HashSet<Runnable>();
	
	static final long DYNAMIC_ENTITY_FLAG = 1;
	static final boolean isEntityMoving( Entity e ) {
		return e.vx != 0 || e.vy != 0 || e.vz != 0 || e.ax != 0 || e.ay != 0 || e.az != 0;
	}
	
	protected long getNextPhysicsUpdateTime() {
		return (entityIndex.entityTree.flags & DYNAMIC_ENTITY_FLAG) == DYNAMIC_ENTITY_FLAG ? currentTime + physicsInterval : TIME_INFINITY;
	}
	
	@Override public long getNextAutomaticUpdateTime() {
		return Math.min(getNextPhysicsUpdateTime(), super.getNextAutomaticUpdateTime());
	}
	
	@Override
	protected void passTime(final long targetTime) {
		long sysTime = System.currentTimeMillis();
		System.err.println( (targetTime - currentTime) + " milliseconds passed to "+targetTime );
		System.err.println( "Lag: "+(sysTime - targetTime));
		entityIndex = entityIndex.updateEntities( DYNAMIC_ENTITY_FLAG, new EntityUpdater() {
			protected double damp( double v, double min ) {
				return Math.abs(v) < min ? 0 : v;
			}
			
			@Override public Entity update( Entity e, EntityShell shell ) {
				e = e.withUpdatedPosition(targetTime);
				if( e != null && e.y <= e.radius && e.vy <= 0 ) {
					if( Math.abs(e.vy) < gravity/10 ) {
						e = e.withPosition(
							e.time,
							e.x, e.radius, e.z,
							damp(e.vx * 0.9, 0.1), 0, e.vz,
							e.ax, 0, e.az
						);
					} else {
						e = e.withPosition(
							e.time,
							e.x, e.y, e.z,
							damp(e.vx * 0.9, 0.1), e.vy*-0.9, e.vz,
							e.ax, e.ay, e.az
						);
					}
				}
				if( e != null ) {
					e = e.behavior.onMove( targetTime, e, shell );
				}
				return e;
			}
		} );
		entityIndex = entityIndex.updateEntities( DYNAMIC_ENTITY_FLAG, new EntityUpdater() {
			Entity collisionCheckEntity;
			Entity collisionTargetEntity;
			Visitor<Entity> collisionChecker = new Visitor<Entity>() {
				@Override public void visit( Entity v ) {
					if( v == collisionCheckEntity ) return;
					
					if( solidCollision(collisionCheckEntity, v) ) {
						collisionTargetEntity = v;
					}
				}
			};
			@Override
			public Entity update( Entity e, EntityShell shell ) {
				collisionCheckEntity = e;
				collisionTargetEntity = null;
				entityIndex.forEachEntityIntersecting( e.boundingBox, collisionChecker );
				return collisionTargetEntity == null ? e : 
					e.behavior.onCollision( targetTime, e, collisionTargetEntity, shell );
			}
		} );
		currentTime = targetTime;
		for( Runnable r : worldUpdateListeners ) r.run();
	}
	
	final static double gravity = 400;
	
	@Override
	protected void handleEvent(Object evt) {
		entityIndex = entityIndex.updateEntities(DYNAMIC_ENTITY_FLAG, new EntityUpdater() {
			@Override
			public Entity update( Entity e, EntityShell shell ) {
				if( e.radius >= 2 && Math.random() < 0.1 ) {
					// Asplode!
					for( int i=0; i<4; ++i ) {
						shell.add( new Entity(
							null, e.time,
							e.x, e.y, e.z,
							e.vx + Math.random()*200-100, e.vy + Math.random()*400-200, 0, //e.vz + Math.random()*200-100,
							e.ax, -gravity, e.az,
							e.radius/2, e.mass/4, e.color,
							DYNAMIC_ENTITY_FLAG, CoolEntityBehavior.INSTANCE
						) );
					}
					return null;
				} else {
					return e;
				}
			}
		});
	}
	
	static class CoolEntityBehavior implements EntityBehavior {
		static final CoolEntityBehavior INSTANCE = new CoolEntityBehavior(0);
		final long lastCollisionTime;
		
		public CoolEntityBehavior( long lastCollisionTime ) {
			this.lastCollisionTime = lastCollisionTime;
		}
		
		protected Entity withColorAndBehavior( Entity e, Color c, EntityBehavior b ) {
			return new Entity(
				e.tag, e.time,
				e.x, e.y, e.z, e.vx, e.vy, e.vz, e.ax, e.ay, e.az,
				e.radius, e.mass, c,
				DYNAMIC_ENTITY_FLAG, b
			);
		}
		
		@Override public Entity onMove( long time, Entity self,	EntityShell shell ) {
			if( self.tag == "tag" ) {
				System.err.println("Time since last collision "+(time - lastCollisionTime));
			}
			return (self.color == Color.RED && (time - lastCollisionTime >= 1000) ) ?
				withColorAndBehavior( self, self.tag == "tag" ? Color.GREEN : Color.WHITE, this ) : self;
		}
		
		@Override public Entity onCollision( long time, Entity self, Entity other, EntityShell shell ) {
			
			/*
			 * An unrealistic bouncing algorithm.
			 * Doesn't take relative velocity or momentum into account.
			 * Just reflects velocity vectors.
			 */
			
			assert self != null;
			assert other != null;
			double dx = self.x - other.x, dy = self.y - other.y, dz = self.z - other.z;
			double distSquared = dx*dx + dy*dy + dz*dz;
			if( distSquared == 0 ) return self;
			
			double dist = Math.sqrt(distSquared);
			double nx = dx/dist, ny = dy/dist, nz = dz/dist; // Normalized distance
			
			double dotProduct = nx*self.vx + ny*self.vy + nz*self.vz;
			double
				rx = 2*dotProduct*nx - self.vx,
				ry = 2*dotProduct*ny - self.vy,
				rz = 2*dotProduct*nz - self.vz;
			
			double rad = self.radius+other.radius;
			double overlap = rad-dist;
			assert overlap >= 0;
			return new Entity(
				self.tag, time,
				self.x + overlap*nx, self.y + overlap*ny, self.z + overlap*nz,
				-rx*0.9, -ry*0.9, -rz*0.9,
				self.ax, self.ay, self.az,
				self.radius, self.mass,
				self.tag == "tag" ? Color.ORANGE : Color.RED,
				self.flags, new CoolEntityBehavior(time)
			);
		}
	};
	
	public static void main( String[] args ) throws Exception {
		final Simulator sim = new Simulator();
		final QueuelessRealTimeEventSource<Object> eventSource = new QueuelessRealTimeEventSource<Object>();
		
		for( int i=0; i<200; ++i ) {
			Entity e = new Entity(
				i == 0 ? "tag" : null, eventSource.getCurrentTime(),
				Math.random()*500-250, Math.random()*1000, 0,
				Math.random()*20-10, Math.random()*200-10, 0,
				0, -gravity, 0,
				10, 1, i == 0 ? Color.GREEN : Color.WHITE,
				DYNAMIC_ENTITY_FLAG, CoolEntityBehavior.INSTANCE
			);
			sim.entityIndex.add(e);
		}
		for( int i=0; i<1600; ++i ) {
			Entity e = new Entity(
				i == 0 ? "tag" : null, eventSource.getCurrentTime(),
				Math.random()*8000-2000, Math.random()*2000, 0,
				0, 0, 0,
				0, 0, 0,
				20, 1000, Color.BLUE,
				0, NULL_BEHAVIOR
			);
			sim.entityIndex.add(e);
		}
		
		final Frame frame = new Frame();
		final Canvas canv = new Canvas() {
			private static final long serialVersionUID = 1L;
			
			BufferedImage bufferCache = null;
			protected BufferedImage getBuffer( int width, int height ) {
				if( bufferCache == null || bufferCache.getWidth() != width || bufferCache.getHeight() != height ) {
					bufferCache = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
				}
				return bufferCache;
			}
			
			public void paint(Graphics _g) {
				BufferedImage buf = getBuffer(getWidth(), getHeight());
				
				final Graphics g = buf.getGraphics();
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, getWidth(), getHeight());
				AABB screenBounds = new AABB(-getWidth()/2, 0, Double.NEGATIVE_INFINITY, getWidth()/2, getHeight(), Double.POSITIVE_INFINITY );
				sim.entityIndex.forEachEntityIntersecting(screenBounds, new Visitor<Entity>() {
					public void visit(Entity e) {
						g.setColor( e.color );
						g.fillOval( (int)(e.x-e.radius) + getWidth()/2, (int)(getHeight()-e.y-e.radius), (int)(e.radius*2), (int)(e.radius*2) );
					};
				});
				
				_g.drawImage( buf, 0, 0, null ); 
			};
			public void update(Graphics g) {
				paint(g);
			}
		};
		canv.setPreferredSize(new Dimension(640,480));
		frame.add(canv);
		frame.pack();
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setVisible(true);
		canv.addKeyListener( new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				try {
					eventSource.post(new Object());
				} catch( InterruptedException e1 ) {
					Thread.currentThread().interrupt();
					e1.printStackTrace();
				}
			}
		});
		canv.requestFocus();

		sim.worldUpdateListeners.add(new Runnable() {
			@Override public void run() {
				canv.repaint();
			}
		});
		
		sim.currentTime = eventSource.getCurrentTime();
		EventLoop.run(eventSource, sim);
	}
}
