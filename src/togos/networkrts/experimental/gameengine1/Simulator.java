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
import java.util.HashSet;

import togos.networkrts.experimental.gensim.BaseMutableAutoUpdatable;
import togos.networkrts.experimental.gensim.EventLoop;
import togos.networkrts.experimental.gensim.QueuelessRealTimeEventSource;

public class Simulator extends BaseMutableAutoUpdatable<Object>
{
	static class Entity
	{
		public final Object tag;
		public final Entity parent;
		public final long time;
		public final double x, y, z, spaceRadius, solidRadius;
		public final double vx, vy, vz;
		public final double ax, ay, az;
		public final double mass;
		public final Color color;
		public final EntityBehavior behavior;
		
		public final AABB boundingBox;
		public final boolean isMoving;
		public final boolean isSolid;
		
		public Entity(
			Object tag, Entity parent, long time,
			double x, double y, double z,
			double vx, double vy, double vz,
			double ax, double ay, double az,
			double spaceRadius, double solidRadius,
			double mass, Color color, EntityBehavior behavior
		) {
			this.tag = tag;
			this.parent = parent;
			this.time = time;
			this.x = x; this.y = y; this.z = z;
			this.vx = vx; this.vy = vy; this.vz = vz;
			this.ax = ax; this.ay = ay; this.az = az;
			this.spaceRadius = spaceRadius; this.solidRadius = solidRadius;
			this.mass = mass; this.color = color;
			this.behavior = behavior;
			this.boundingBox = new AABB(
				x-solidRadius, y-solidRadius, z-solidRadius,
				x+solidRadius, y+solidRadius, z+solidRadius
			);
			this.isMoving = vx != 0 || vy != 0 || vz != 0 || ax != 0 || ay != 0 || az != 0;
			this.isSolid = solidRadius > 0;
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
				tag, parent, time,
				x, y, z, vx, vy, vz, ax, ay, az,
				spaceRadius, solidRadius,
				mass, color, behavior
			);
		}
	}
	
	interface EntityShell
	{
		public void add( Entity e );
	}
	
	interface EntityUpdater
	{
		public Entity update( Entity e, EntityShell shell );
	}
	
	interface EntityBehavior
	{
		public Entity onMove( long time, Entity self, EntityShell shell );
		public Entity onCollision( long time, Entity self, Entity other, EntityShell shell );
	}
	
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
	
	interface SpatialIndex<Item> {
		public void forEachItemIntersecting( AABB bounds, Visitor<Item> callback );
	}	
	
	protected static final boolean solidCollision( Entity e1, Entity e2 ) {
		assert e1 != e2;
		
		double dx = e1.x-e2.x, dy = e1.y-e2.y, dz = e1.z-e2.z;
		double distSquared = dx*dx + dy*dy + dz*dz;
		double radSum = e1.solidRadius + e2.solidRadius;
		return distSquared < radSum*radSum;
	}
	
	static class EntityIndex implements SpatialIndex<Entity>
	{
		private final HashSet<Entity> allEntities = new HashSet<Entity>();
		private int movingEntityCount = 0;
		
		public boolean hasMovingEntities() {
			return movingEntityCount > 0;
		}
		
		public void add(Entity e) {
			allEntities.add(e);
			if( e.isMoving ) ++movingEntityCount;
		}
		
		@Override public void forEachItemIntersecting(AABB bounds, Visitor<Entity> callback) {
			for( Entity e : allEntities ) {
				if( e.boundingBox.intersects(bounds) ) callback.visit(e);
			}
		}
		
		public EntityIndex updateEntities( EntityUpdater u ) {
			final EntityIndex newIndex = new EntityIndex();
			final EntityShell shell = new EntityShell() {
				@Override public void add( Entity e ) {
					newIndex.add(e);
				}
			};
			for( Entity e : allEntities ) {
				if( (e = u.update(e, shell)) != null ) {
					newIndex.add(e);
				}
			}
			return newIndex;
		}
	}
	
	EntityIndex entityIndex = new EntityIndex();
	public long physicsInterval = 50;
	HashSet<Runnable> worldUpdateListeners = new HashSet<Runnable>();
	
	protected long getNextPhysicsUpdateTime() {
		return entityIndex.hasMovingEntities() ? currentTime + physicsInterval : TIME_INFINITY;
	}
	
	@Override public long getNextAutomaticUpdateTime() {
		return Math.min(getNextPhysicsUpdateTime(), super.getNextAutomaticUpdateTime());
	}
	
	@Override
	protected void passTime(final long targetTime) {
		System.err.println( (targetTime - currentTime) + " milliseconds passed to "+targetTime );
		System.err.println( "Moving entities: "+entityIndex.movingEntityCount );
		entityIndex = entityIndex.updateEntities( new EntityUpdater() {
			protected double damp( double v, double min ) {
				return Math.abs(v) < min ? 0 : v;
			}
			
			@Override public Entity update( Entity e, EntityShell shell ) {
				e = e.withUpdatedPosition(targetTime);
				if( e != null && e.y <= e.solidRadius && e.vy <= 0 ) {
					if( Math.abs(e.vy) < 20 ) {
						e = e.withPosition(
							e.time,
							e.x, e.solidRadius, e.z,
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
				entityIndex.forEachItemIntersecting( e.boundingBox, collisionChecker );
				return collisionTargetEntity == null ? e : 
					e.behavior.onCollision( targetTime, e, collisionTargetEntity, shell );
			}
		} );
		currentTime = targetTime;
		for( Runnable r : worldUpdateListeners ) r.run();
	}

	@Override
	protected void handleEvent(Object evt) {
		// TODO: bounce
	}
	
	static class CoolEntityBehavior implements EntityBehavior {
		final long lastCollisionTime;
		
		public CoolEntityBehavior( long lastCollisionTime ) {
			this.lastCollisionTime = lastCollisionTime;
		}
		
		protected Entity withColorAndBehavior( Entity e, Color c, EntityBehavior b ) {
			return new Entity(
				e.tag, e.parent, e.time,
				e.x, e.y, e.z, e.vx, e.vy, e.vz, e.ax, e.ay, e.az,
				e.spaceRadius, e.solidRadius, e.mass, c, b
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
		
		CoolEntityBehavior beh = new CoolEntityBehavior(0);
		for( int i=0; i<100; ++i ) {
			Entity e = new Entity(
				i == 0 ? "tag" : null, null, eventSource.getCurrentTime(),
				Math.random()*200-100, Math.random()*200, 0,
				Math.random()*20-10, Math.random()*200-10, 0,
				0, -100, 0,
				10, 10,
				1, i == 0 ? Color.GREEN : Color.WHITE, beh
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
				
				Graphics g = buf.getGraphics();
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, getWidth(), getHeight());
				for( Entity e : sim.entityIndex.allEntities ) {
					g.setColor( e.color );
					g.fillOval( (int)(e.x-e.solidRadius) + getWidth()/2, (int)(getHeight()-e.y-e.solidRadius), (int)(e.solidRadius*2), (int)(e.solidRadius*2) );
				}
				
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
