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
		public static final long FLAG_EXISTING = 0x00000001;
		public static final long FLAG_MOVING = 0x00000002;
		
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
			double radius,
			double mass, Color color, EntityBehavior behavior
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
			long _flags = FLAG_EXISTING;
			if( vx != 0 || vy != 0 || vz != 0 || ax != 0 || ay != 0 || az != 0 ) _flags |= FLAG_MOVING;
			flags = _flags;
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
				radius, mass, color, behavior
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
			minX = x0;  maxX = x1;
			minY = y0;  maxY = y1;
			minZ = z0;  maxZ = z1;
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
			
			public EntityTreeNode( double minX, double minY, double minZ, double maxX, double maxY, double maxZ ) {
				super( minX, minY, minZ, maxX, maxY, maxZ );
			}
			public EntityTreeNode() {
				this(
					Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
					Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY
				);
			}
			
			private void subDivide() {
				ArrayList<Entity> oldEntities = localEntities;
				double
					bMinX = maxX, bMinY = maxY, bMinZ = maxZ,
					bMaxX = minX, bMaxY = minY, bMaxZ = minZ;
				for( Entity e : oldEntities ) {
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
					if( !subNodeA.add(e) ) if( !subNodeB.add(e) ) localEntities.add(e);
				}
			}
			
			public boolean add(Entity e) {
				if( !contains(e.boundingBox) ) return false;
				
				flags |= e.flags;
				totalEntityCount += 1;
				
				if( this.subNodeA != null ) {
					// Already subdivided
					return subNodeA.add(e) || subNodeB.add(e) || localEntities.add(e);
				} else if( localEntities.size() < 32 ) {
					return localEntities.add(e);
				} else {
					localEntities.add(e);
					subDivide();
					return true;
				}
			}
			
			public void forEachEntity( long requireFlags, Visitor<Entity> callback ) {
				if( (flags & requireFlags) == 0 ) return;
				
				for( Entity e : localEntities ) {
					if( (e.flags & requireFlags) != 0 ) callback.visit(e);
				}
				if( this.subNodeA != null ) {
					subNodeA.forEachEntity( requireFlags, callback );
					subNodeB.forEachEntity( requireFlags, callback );
				}
			}
			
			public void forEachEntityIntersecting( AABB bounds, Visitor<Entity> callback ) {
				if( !intersects(bounds) ) return;
				
				for( Entity e : localEntities ) {
					if( e.boundingBox.intersects(bounds) ) callback.visit(e);
				}
				if( this.subNodeA != null ) {
					subNodeA.forEachEntityIntersecting(bounds, callback);
					subNodeB.forEachEntityIntersecting(bounds, callback);
				}
			}
			
			/*
			 * I suppose if we are treating trees as immutable once constructed (with add()),
			 * that this should return a new TreeNode if there are any changes (including additions)
			public EntityTreeNode update( long requireFlags, EntityUpdater u, EntityShell shell ) {
				if( (flags & requireFlags) == 0 ) return this;
				
				for( Entity e : localEntities ) {
					if( u.update(e);
				}
			}
			*/
		}
		
		final EntityTreeNode entityTree = new EntityTreeNode();
		
		// TODO: Remove this, just provide flags.
		public boolean hasMovingEntities() {
			return (entityTree.flags & Entity.FLAG_MOVING) != 0;
		}
		
		public void add(Entity e) {
			entityTree.add(e);
		}
		
		public void forEachEntityIntersecting(AABB bounds, Visitor<Entity> callback) {
			entityTree.forEachEntityIntersecting(bounds, callback);
		}
		
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
	}
	
	EntityIndex entityIndex = new EntityIndex();
	public long physicsInterval = 10;
	HashSet<Runnable> worldUpdateListeners = new HashSet<Runnable>();
	
	protected long getNextPhysicsUpdateTime() {
		return entityIndex.hasMovingEntities() ? currentTime + physicsInterval : TIME_INFINITY;
	}
	
	@Override public long getNextAutomaticUpdateTime() {
		return Math.min(getNextPhysicsUpdateTime(), super.getNextAutomaticUpdateTime());
	}
	
	@Override
	protected void passTime(final long targetTime) {
		long sysTime = System.currentTimeMillis();
		System.err.println( (targetTime - currentTime) + " milliseconds passed to "+targetTime );
		System.err.println( entityIndex.entityTree.totalEntityCount + " entities" +(entityIndex.hasMovingEntities() ? ", some moving" : "") );
		System.err.println( "Lag: "+(sysTime - targetTime));
		entityIndex = entityIndex.updateEntities( new EntityUpdater() {
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
		entityIndex = entityIndex.updateEntities( new EntityUpdater() {
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
		entityIndex = entityIndex.updateEntities(new EntityUpdater() {
			@Override
			public Entity update( Entity e, EntityShell shell ) {
				if( e.radius >= 2 && Math.random() < 0.1 ) {
					// Asplode!
					for( int i=0; i<4; ++i ) {
						shell.add( new Entity(
							null, e.time,
							e.x, e.y, e.z,
							e.vx + Math.random()*200-100, e.vy + Math.random()*400-200, e.vz + Math.random()*200-100,
							e.ax, -gravity, e.az,
							e.radius/2, e.mass/4, e.color, CoolEntityBehavior.INSTANCE
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
				e.radius, e.mass, c, b
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
			assert self != null;
			assert other != null;
			if( self.tag == "tag" ) {
				System.err.println("Collision!");
			}
			return withColorAndBehavior( self, self.tag == "tag" ? Color.ORANGE : Color.RED, new CoolEntityBehavior(time) );
		}
	};
	
	public static void main( String[] args ) throws Exception {
		final Simulator sim = new Simulator();
		final QueuelessRealTimeEventSource<Object> eventSource = new QueuelessRealTimeEventSource<Object>();
		
		for( int i=0; i<200; ++i ) {
			Entity e = new Entity(
				i == 0 ? "tag" : null, eventSource.getCurrentTime(),
				Math.random()*200-100, Math.random()*200, 0,
				Math.random()*20-10, Math.random()*200-10, 0,
				0, -gravity, 0,
				10, 1, i == 0 ? Color.GREEN : Color.WHITE, CoolEntityBehavior.INSTANCE
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
