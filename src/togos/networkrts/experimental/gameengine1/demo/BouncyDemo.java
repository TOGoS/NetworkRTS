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

public class BouncyDemo extends BaseMutableAutoUpdatable<BouncyDemo.Signal>
{
	enum Signal {
		BREAK, JUMP
	}
	
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
							e.ax, e.ay, e.az
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
	protected void handleEvent(final Signal evt) {
		entityIndex = entityIndex.updateEntities(DYNAMIC_ENTITY_FLAG, new EntityUpdater<Bouncer>() {
			@Override
			public Bouncer update( Bouncer e, EntityShell<Bouncer> shell ) {
				if( evt == Signal.BREAK ) {
					if( e.radius >= 2 && Math.random() < 0.1 ) {
						// Asplode!
						for( int i=0; i<4; ++i ) {
							shell.add( new Bouncer(
								null, e.time,
								e.x, e.y, e.z,
								e.vx + Math.random()*200-100, e.vy + Math.random()*400-200, 0, //e.vz + Math.random()*200-100,
								e.ax, e.ay, e.az,
								e.radius/2, e.mass/4, e.color,
								DYNAMIC_ENTITY_FLAG, CoolEntityBehavior.INSTANCE
							) );
						}
						return null;
					}
				}
					
				if( evt == Signal.JUMP ) {
					if( e.ay < 0 ) {
						return e.withPosition(
							e.time, e.x, e.y, e.z,
							e.vx, e.vy + Math.random() * 100, e.vz,
							e.ax, e.ay, e.az
						);
					}
				}
				
				return e;
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
		
		protected static final double pos( double x, double vx, double ax, double dt ) {
			return x + vx*dt + ax*dt*dt/2;
		}
		
		@Override public Bouncer onCollision( long time, Bouncer self, Bouncer other, EntityShell shell ) {
			assert self != null;
			assert other != null;
			
			double sdt0 = (time -  self.time)/1000d;
			double odt0 = (time - other.time)/1000d;
			
			double dt = 0.01;
			double sdt1 = sdt0+dt;
			double odt1 = odt0+dt;
			
			double sx0 = pos( self.x,  self.vx,  self.ax, sdt0);
			double sy0 = pos( self.y,  self.vy,  self.ay, sdt0);
			double sz0 = pos( self.z,  self.vz,  self.az, sdt0);
			double sx1 = pos( self.x,  self.vx,  self.ax, sdt1);
			double sy1 = pos( self.y,  self.vy,  self.ay, sdt1);
			double sz1 = pos( self.z,  self.vz,  self.az, sdt1);
			double ox0 = pos(other.x, other.vx, other.ax, odt0);
			double oy0 = pos(other.y, other.vy, other.ay, odt0);
			double oz0 = pos(other.z, other.vz, other.az, odt0);
			double ox1 = pos(other.x, other.vx, other.ax, odt1);
			double oy1 = pos(other.y, other.vy, other.ay, odt1);
			double oz1 = pos(other.z, other.vz, other.az, odt1);
			
			double dx0 = sx0 - ox0, dy0 = sy0 - oy0, dz0 = sz0 - oz0;
			double dist0Squared = dx0*dx0 + dy0*dy0 + dz0*dz0;
			if( dist0Squared == 0 ) return self;
			
			double dx1 = sx1 - ox1, dy1 = sy1 - oy1, dz1 = sz1 - oz1;
			double dist1Squared = dx1*dx1 + dy1*dy1 + dz1*dz1;

			// Inverse proportion of our mass over total system mass
			double mProp = other.mass/(self.mass+other.mass);
			double rad = self.radius+other.radius;
			double dist0 = Math.sqrt(dist0Squared);
			double overlap0 = rad-dist0;
			if( overlap0 < 0 ) return self;
			
			double dist1 = Math.sqrt(dist1Squared);
			double overlap1 = rad-dist1;
			
			double dpm = 0.5;
			double dvm = overlap1 > overlap0 ? 1.5 * (overlap1 - overlap0) / dt : 0;
			
			// New method?
			// Push along centerline to border, proportional to mass
			// Calculate how much overlap 1s in future
			// Add that much to velocity along centerline * 1+elasticity (elasticity in 0-1) 
			
			return new Bouncer(
				self.tag, time,
				sx0 + dpm * dx0/dist0 * mProp,
				sy0 + dpm * dy0/dist0 * mProp,
				sz0 + dpm * dz0/dist0 * mProp,
				self.vx + mProp * dvm * (dx0/dist0),
				self.vy + mProp * dvm * (dy0/dist0),
				self.vz + mProp * dvm * (dz0/dist0),
				self.ax, self.ay, self.az,
				self.radius, self.mass,
				self.color == Color.BLUE ? self.color : self.tag == "tag" ? Color.ORANGE : Color.RED,
				self.flags, new CoolEntityBehavior(time)
			);
		}
	};
	
	public static void main( String[] args ) throws Exception {
		final BouncyDemo sim = new BouncyDemo();
		final QueuelessRealTimeEventSource<Signal> eventSource = new QueuelessRealTimeEventSource<Signal>();
		
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
		for( int i=0; i<800; ++i ) {
			Bouncer e = new Bouncer(
				null, eventSource.getCurrentTime(),
				Math.random()*8000-2000, Math.random()*2000, 0,
				0, 0, 0,
				0, +gravity*0.005, 0,
				10+Math.random()*100, 1, Color.BLUE,
				DYNAMIC_ENTITY_FLAG, CoolEntityBehavior.INSTANCE
				//0, NULL_BEHAVIOR
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
					switch(e.getKeyCode()) {
					case KeyEvent.VK_SPACE: eventSource.post(Signal.BREAK); break;
					case KeyEvent.VK_ENTER: eventSource.post(Signal.JUMP); break;
					}
					
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
