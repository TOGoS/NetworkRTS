package togos.networkrts.experimental.gameengine1.demo;

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

import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.Entity;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.experimental.gameengine1.index.EntityShell;
import togos.networkrts.experimental.gameengine1.index.EntityUpdater;
import togos.networkrts.experimental.gameengine1.index.Visitor;
import togos.networkrts.experimental.gensim.BaseMutableAutoUpdatable;
import togos.networkrts.experimental.gensim.EventLoop;
import togos.networkrts.experimental.gensim.QueuelessRealTimeEventSource;

public class BouncyDemo extends BaseMutableAutoUpdatable<Object>
{
	static class Bouncer extends Entity
	{
		public final long time;
		public final double x, y, z;
		public final double vx, vy, vz;
		public final double ax, ay, az;
		public final double radius;
		public final double mass;
		public final Color color;
		public final EntityBehavior<Bouncer> behavior;
				
		public Bouncer(
			Object tag, long time,
			double x, double y, double z,
			double vx, double vy, double vz,
			double ax, double ay, double az,
			double radius,	double mass, Color color,
			long flags, EntityBehavior behavior
		) {
			super( tag, flags, x-radius, y-radius, z-radius, x+radius, y+radius, z+radius );
			this.time = time;
			this.x = x; this.y = y; this.z = z;
			this.vx = vx; this.vy = vy; this.vz = vz;
			this.ax = ax; this.ay = ay; this.az = az;
			this.radius = radius;
			this.mass = mass; this.color = color;
			this.behavior = behavior;
		}
		
		public Bouncer withUpdatedPosition( long targetTime ) {
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
		
		public Bouncer withPosition( long time,
			double x, double y, double z,
			double vx, double vy, double vz,
			double ax, double ay, double az
		) {
			return new Bouncer(
				tag, time,
				x, y, z, vx, vy, vz, ax, ay, az,
				radius, mass, color, flags, behavior
			);
		}
	}
	
	interface EntityBehavior<EC extends Entity>
	{
		public EC onMove( long time, EC self, EntityShell<EC> shell );
		public EC onCollision( long time, EC self, Bouncer other, EntityShell<EC> shell );
	}
	
	static final EntityBehavior NULL_BEHAVIOR = new EntityBehavior<Entity>() {
		@Override public Entity onMove( long time, Entity self, EntityShell<Entity> shell ) { return self; }
		@Override public Entity onCollision( long time, Entity self, Bouncer other, EntityShell<Entity> shell ) { return self; }
	};
	
	protected static final boolean solidCollision( Bouncer e1, Bouncer e2 ) {
		assert e1 != e2;
		
		double dx = e1.x-e2.x, dy = e1.y-e2.y, dz = e1.z-e2.z;
		double distSquared = dx*dx + dy*dy + dz*dz;
		double radSum = e1.radius + e2.radius;
		return distSquared < radSum*radSum;
	}
	
	EntitySpatialTreeIndex entityIndex = new EntitySpatialTreeIndex();
	public long physicsInterval = 10;
	HashSet<Runnable> worldUpdateListeners = new HashSet<Runnable>();
	
	static final long DYNAMIC_ENTITY_FLAG = 1;
	static final boolean isEntityMoving( Bouncer e ) {
		return e.vx != 0 || e.vy != 0 || e.vz != 0 || e.ax != 0 || e.ay != 0 || e.az != 0;
	}
	
	protected long getNextPhysicsUpdateTime() {
		return (entityIndex.getAllEntityFlags() & DYNAMIC_ENTITY_FLAG) == DYNAMIC_ENTITY_FLAG ? currentTime + physicsInterval : TIME_INFINITY;
	}
	
	@Override public long getNextAutomaticUpdateTime() {
		return Math.min(getNextPhysicsUpdateTime(), super.getNextAutomaticUpdateTime());
	}
	
	@Override
	protected void passTime(final long targetTime) {
		long sysTime = System.currentTimeMillis();
		System.err.println( (targetTime - currentTime) + " milliseconds passed to "+targetTime );
		System.err.println( "Lag: "+(sysTime - targetTime));
		entityIndex = entityIndex.updateEntities( DYNAMIC_ENTITY_FLAG, new EntityUpdater<Bouncer>() {
			protected double damp( double v, double min ) {
				return Math.abs(v) < min ? 0 : v;
			}
			
			@Override public Bouncer update( Bouncer e, EntityShell<Bouncer> shell ) {
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
		entityIndex = entityIndex.updateEntities( DYNAMIC_ENTITY_FLAG, new EntityUpdater<Bouncer>() {
			Bouncer collisionCheckEntity;
			Bouncer collisionTargetEntity;
			Visitor<Bouncer> collisionChecker = new Visitor<Bouncer>() {
				@Override public void visit( Bouncer v ) {
					if( v == collisionCheckEntity ) return;
					
					if( solidCollision(collisionCheckEntity, v) ) {
						collisionTargetEntity = v;
					}
				}
			};
			@Override
			public Bouncer update( Bouncer e, EntityShell shell ) {
				collisionCheckEntity = e;
				collisionTargetEntity = null;
				entityIndex.forEachEntityIntersecting( e, collisionChecker );
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
		entityIndex = entityIndex.updateEntities(DYNAMIC_ENTITY_FLAG, new EntityUpdater<Bouncer>() {
			@Override
			public Bouncer update( Bouncer e, EntityShell<Bouncer> shell ) {
				if( e.radius >= 2 && Math.random() < 0.1 ) {
					// Asplode!
					for( int i=0; i<4; ++i ) {
						shell.add( new Bouncer(
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
	
	static class CoolEntityBehavior implements EntityBehavior<Bouncer> {
		static final CoolEntityBehavior INSTANCE = new CoolEntityBehavior(0);
		final long lastCollisionTime;
		
		public CoolEntityBehavior( long lastCollisionTime ) {
			this.lastCollisionTime = lastCollisionTime;
		}
		
		protected Bouncer withColorAndBehavior( Bouncer e, Color c, EntityBehavior b ) {
			return new Bouncer(
				e.tag, e.time,
				e.x, e.y, e.z, e.vx, e.vy, e.vz, e.ax, e.ay, e.az,
				e.radius, e.mass, c,
				DYNAMIC_ENTITY_FLAG, b
			);
		}
		
		@Override public Bouncer onMove( long time, Bouncer self,	EntityShell<Bouncer> shell ) {
			if( self.tag == "tag" ) {
				System.err.println("Time since last collision "+(time - lastCollisionTime));
			}
			return (self.color == Color.RED && (time - lastCollisionTime >= 1000) ) ?
				withColorAndBehavior( self, self.tag == "tag" ? Color.GREEN : Color.WHITE, this ) : self;
		}
		
		@Override public Bouncer onCollision( long time, Bouncer self, Bouncer other, EntityShell shell ) {
			
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
			return new Bouncer(
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
		final BouncyDemo sim = new BouncyDemo();
		final QueuelessRealTimeEventSource<Object> eventSource = new QueuelessRealTimeEventSource<Object>();
		
		for( int i=0; i<200; ++i ) {
			Bouncer e = new Bouncer(
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
			Bouncer e = new Bouncer(
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
				sim.entityIndex.forEachEntityIntersecting(screenBounds, new Visitor<Bouncer>() {
					public void visit(Bouncer e) {
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
