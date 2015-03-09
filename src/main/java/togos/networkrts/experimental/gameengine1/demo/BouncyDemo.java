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
import java.util.Collection;
import java.util.HashSet;

import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.BaseEntity;
import togos.networkrts.experimental.gameengine1.index.EntityAggregation;
import togos.networkrts.experimental.gameengine1.index.EntityIndex;
import togos.networkrts.experimental.gameengine1.index.EntityRanges;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.experimental.gameengine1.index.EntityUpdater;
import togos.networkrts.experimental.gameengine1.index.Visitor;
import togos.networkrts.experimental.gensim.BaseMutableAutoUpdatable;
import togos.networkrts.experimental.gensim.EventLooper;
import togos.networkrts.experimental.gensim.QueuelessRealTimeEventSource;

public class BouncyDemo extends BaseMutableAutoUpdatable<BouncyDemo.Signal>
{
	enum Signal {
		BREAK, JUMP
	}
	
	static final double targetFramesPerSecond = 33.333; 
	
	static class Bouncer extends BaseEntity
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
			long minBitAddress, long maxBitAddress,
			long time,
			double x, double y, double z,
			double vx, double vy, double vz,
			double ax, double ay, double az,
			double radius,	double mass, Color color,
			EntityBehavior<Bouncer> behavior
		) {
			super( minBitAddress, maxBitAddress, time+1, x-radius, y-radius, z-radius, x+radius, y+radius, z+radius );
			this.time = time;
			this.x = x; this.y = y; this.z = z;
			this.vx = vx; this.vy = vy; this.vz = vz;
			this.ax = ax; this.ay = ay; this.az = az;
			this.radius = radius;
			this.mass = mass; this.color = color;
			this.behavior = behavior;
		}
		
		public Bouncer withUpdatedPosition( long targetTime ) {
			final double duration = (targetTime-time)/targetFramesPerSecond;
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
				minBitAddress, maxBitAddress, time,
				x, y, z, vx, vy, vz, ax, ay, az,
				radius, mass, color, behavior
			);
		}
	}
	
	interface EntityBehavior<EC extends EntityAggregation>
	{
		public EC onMove( long time, EC self, Collection<EC> newEntities );
		public EC onCollision( long time, EC self, Bouncer other, Collection<EC> newEntities );
	}
	
	protected static final boolean solidCollision( Bouncer e1, Bouncer e2 ) {
		assert e1 != e2;
		
		double dx = e1.x-e2.x, dy = e1.y-e2.y, dz = e1.z-e2.z;
		double distSquared = dx*dx + dy*dy + dz*dz;
		double radSum = e1.radius + e2.radius;
		return distSquared < radSum*radSum;
	}
	
	EntitySpatialTreeIndex<Bouncer> entityIndex = new EntitySpatialTreeIndex<Bouncer>();
	HashSet<Runnable> worldUpdateListeners = new HashSet<Runnable>();
	
	static final long DYNAMIC_ENTITY_FLAG = 0x01;
	static final long TAGGED_ENTITY_FLAG  = 0x02;
	
	static final boolean isEntityMoving( Bouncer e ) {
		return e.vx != 0 || e.vy != 0 || e.vz != 0 || e.ax != 0 || e.ay != 0 || e.az != 0;
	}
	
	@Override public long getNextAutoUpdateTime() {
		return Math.min(entityIndex.getNextAutoUpdateTime(), super.getNextAutoUpdateTime());
	}
	
	@Override
	protected void passTime(final long targetTime) {
		System.err.println( (targetTime - currentTime) + " ticks passed to "+targetTime );
		entityIndex = entityIndex.updateEntities( EntityRanges.BOUNDLESS, new EntityUpdater<Bouncer>() {
			protected double damp( double v, double min ) {
				return Math.abs(v) < min ? 0 : v;
			}
			
			@Override public Bouncer update( Bouncer e, Collection<Bouncer> newEntities ) {
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
					e = e.behavior.onMove( targetTime, e, newEntities );
				}
				return e;
			}
		} );
		entityIndex = entityIndex.updateEntities( EntityRanges.BOUNDLESS, new EntityUpdater<Bouncer>() {
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
			@Override public Bouncer update( Bouncer e, Collection<Bouncer> newEntities ) {
				collisionCheckEntity = e;
				collisionTargetEntity = null;
				entityIndex.forEachEntity( EntityRanges.forAabb(e.getAabb()), collisionChecker );
				return collisionTargetEntity == null ? e : 
					e.behavior.onCollision( targetTime, e, collisionTargetEntity, newEntities );
			}
		} );
		currentTime = targetTime;
		for( Runnable r : worldUpdateListeners ) r.run();
	}
	
	final static double gravity = 400;
	
	@Override protected void handleEvents(Collection<Signal> events) {
		for( Signal evt : events ) handleEvent(evt);
	};
	
	protected void handleEvent(final Signal evt) {
		entityIndex = entityIndex.updateEntities(EntityRanges.BOUNDLESS, new EntityUpdater<Bouncer>() {
			@Override
			public Bouncer update( Bouncer e, Collection<Bouncer> newEntities ) {
				if( evt == Signal.BREAK ) {
					if( e.radius >= 2 && Math.random() < 0.1 ) {
						// Asplode!
						for( int i=0; i<4; ++i ) {
							newEntities.add( new Bouncer(
								e.minBitAddress, e.maxBitAddress, e.time,
								e.x, e.y, e.z,
								e.vx + Math.random()*200-100, e.vy + Math.random()*400-200, 0, //e.vz + Math.random()*200-100,
								e.ax, e.ay, e.az,
								e.radius/2, e.mass/4, e.color,
								CoolEntityBehavior.INSTANCE
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
		
		protected Bouncer withColorAndBehavior( Bouncer e, Color c, EntityBehavior<Bouncer> b ) {
			return new Bouncer(
				e.minBitAddress, e.maxBitAddress, e.time,
				e.x, e.y, e.z, e.vx, e.vy, e.vz, e.ax, e.ay, e.az,
				e.radius, e.mass, c,
				b
			);
		}
		
		@Override public Bouncer onMove( long time, Bouncer self, Collection<Bouncer> newEntities ) {
			return (self.color == Color.RED && (time - lastCollisionTime >= 1000) ) ?
				withColorAndBehavior( self, Color.WHITE, this ) : self;
		}
		
		protected static final double pos( double x, double vx, double ax, double dt ) {
			return x + vx*dt + ax*dt*dt/2;
		}
		
		@Override public Bouncer onCollision( long time, Bouncer self, Bouncer other, Collection<Bouncer> newEntities ) {
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
				self.minBitAddress, self.maxBitAddress, time,
				sx0 + dpm * dx0/dist0 * mProp,
				sy0 + dpm * dy0/dist0 * mProp,
				sz0 + dpm * dz0/dist0 * mProp,
				self.vx + mProp * dvm * (dx0/dist0),
				self.vy + mProp * dvm * (dy0/dist0),
				self.vz + mProp * dvm * (dz0/dist0),
				self.ax, self.ay, self.az,
				self.radius, self.mass,
				self.color == Color.BLUE ? self.color : Color.RED,
				new CoolEntityBehavior(time)
			);
		}
	};
	
	static class BouncyView {
		public double scx, scy;
		public double scale = 1;
		
		public void draw( EntityIndex<Bouncer> entities, AABB screenBounds, final Graphics g ) {
			entities.forEachEntity(EntityRanges.forAabb(screenBounds), new Visitor<Bouncer>() {
				public void visit(Bouncer e) {
					g.setColor( e.color );
					g.fillOval( (int)(scx + scale*(e.x-e.radius)), (int)(scy - scale*(e.y-e.radius)), (int)(e.radius*scale*2), (int)(e.radius*scale*2) );
				};
			});
		}
	}
	
	public static void main( String[] args ) throws Exception {
		final long initialSimTime = 0;
		final BouncyDemo sim = new BouncyDemo();
		sim.currentTime = initialSimTime;
		final QueuelessRealTimeEventSource<Signal> eventSource = new QueuelessRealTimeEventSource<Signal>();
		
		for( int i=0; i<200; ++i ) {
			long flags = DYNAMIC_ENTITY_FLAG | (i == 0 ? TAGGED_ENTITY_FLAG : 0);
			
			Bouncer e = new Bouncer(
				flags, flags, initialSimTime,
				Math.random()*500-250, Math.random()*1000, 0,
				Math.random()*20-10, Math.random()*200-10, 0,
				0, -gravity, 0,
				10, 1, i == 0 ? Color.GREEN : Color.WHITE,
				CoolEntityBehavior.INSTANCE
			);
			sim.entityIndex = sim.entityIndex.with(e);
		}
		for( int i=0; i<800; ++i ) {
			Bouncer e = new Bouncer(
				DYNAMIC_ENTITY_FLAG, DYNAMIC_ENTITY_FLAG, initialSimTime,
				Math.random()*8000-2000, Math.random()*2000, 0,
				0, 0, 0,
				0, +gravity*0.005, 0,
				10+Math.random()*100, 1, Color.BLUE,
				CoolEntityBehavior.INSTANCE
				//0, NULL_BEHAVIOR
			);
			sim.entityIndex = sim.entityIndex.with(e);
		}
		
		final Frame frame = new Frame();
		final BouncyView view = new BouncyView();
		final Canvas canv = new Canvas() {
			private static final long serialVersionUID = 1L;
			
			BufferedImage bufferCache = null;
			protected BufferedImage getBuffer( int width, int height ) {
				if( bufferCache == null || bufferCache.getWidth() != width || bufferCache.getHeight() != height ) {
					bufferCache = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
				}
				return bufferCache;
			}
			
			public void paint(Graphics _g) {
				BufferedImage buf = getBuffer(getWidth(), getHeight());
				
				final Graphics g = buf.getGraphics();
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, getWidth(), getHeight());
				AABB screenBounds = new AABB(-getWidth()/view.scale/2, 0, Double.NEGATIVE_INFINITY, getWidth()/view.scale/2, getHeight()/view.scale, Double.POSITIVE_INFINITY );
				view.scx = getWidth()/2;
				view.scy = getHeight()*0.9;
				view.draw( sim.entityIndex, screenBounds, g );
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
					case KeyEvent.VK_PLUS: case KeyEvent.VK_EQUALS:
						view.scale *= 1.5; break;
					case KeyEvent.VK_MINUS: case KeyEvent.VK_UNDERSCORE:
						view.scale /= 1.5; break;
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
		
		final long stepLength = (long)(1000/targetFramesPerSecond);
		EventLooper<BouncyDemo.Signal> looper = new EventLooper<BouncyDemo.Signal>(eventSource, sim, stepLength);
		looper.run();
	}
}
