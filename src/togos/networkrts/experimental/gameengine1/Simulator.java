package togos.networkrts.experimental.gameengine1;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;

import togos.networkrts.experimental.gensim.BaseMutableAutoUpdatable;
import togos.networkrts.experimental.gensim.EventLoop;
import togos.networkrts.experimental.gensim.QueuelessRealTimeEventSource;
import togos.networkrts.experimental.gensim.RealTimeEventSource;

public class Simulator extends BaseMutableAutoUpdatable<Object>
{
	interface EntityBehavior
	{
		public void onMove( long time, Entity self );
		public void onCollision( long time, Entity self, Entity other );
	}
	
	/** Axis-aligned bounding box */
	static class AABB<PayloadClass> {
		double minX, minY, minZ, maxX, maxY, maxZ;
		public PayloadClass payload;
		
		public AABB( double x0, double y0, double z0, double x1, double y1, double z1, PayloadClass payload ) {
			minX = x0;  maxX = x1;
			minY = y0;  maxY = y1;
			minZ = z0;  maxZ = z1;
			this.payload = payload;
		}
		public AABB( PayloadClass payload ) {
			this.payload = payload;
		}
		public AABB() {}
		
		public final void setCentered( double x, double y, double z, double rad ) {
			minX = x - rad;  maxX = x + rad;
			minY = y - rad;  maxY = y + rad;
			minZ = z - rad;  maxZ = z + rad;
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
	
	interface SpatialIndex<PayloadClass> {
		public void add( AABB<PayloadClass> box );
		public void remove( AABB<PayloadClass> box );
		public void findIntersection( AABB<?> bounds, Visitor<AABB<PayloadClass>> callback );
	}
	
	static class DumbSpatialIndex<PayloadClass> implements SpatialIndex<PayloadClass>
	{
		HashSet<AABB<PayloadClass>> boxes = new HashSet<AABB<PayloadClass>>();
		
		@Override public void add(AABB box) {
			boxes.add(box);
		}
		@Override public void remove(AABB box) {
			boxes.remove(box);
		}
		@Override public void findIntersection(AABB<?> bounds, Visitor<AABB<PayloadClass>> callback) {
			for( AABB box : boxes ) {
				if( box.intersects(bounds) ) callback.visit(box);
			}
		}
	}
	
	static class Space
	{
		final HashSet<Entity> movingEntities = new HashSet();
		final SpatialIndex<Entity> solidIndex = new DumbSpatialIndex();
		
		protected void updateEntity( Entity e ) {
			if( e.isMoving() ) movingEntities.add(e);
			else movingEntities.remove(e);
		}
		
		public void addEntity( Entity e ) {
			solidIndex.add( e.updateBoundingBox() );
			updateEntity( e );
		}
		
		protected boolean solidCollision( Entity e1, Entity e2 ) {
			double dx = e1.x-e2.x, dy = e1.y-e2.y, dz = e1.z-e2.z;
			double distSquared = dx*dx + dy*dy + dz*dz;
			double radSum = e1.solidRadius + e2.solidRadius;
			return distSquared < radSum*radSum;
		}
		
		long currentTime;
		
		Entity collisionCheckEntity;
		Visitor<AABB<Entity>> collisionCallback = new Visitor<Simulator.AABB<Entity>>() {
			@Override public void visit(AABB<Entity> v) {
				if( collisionCheckEntity != v.payload && solidCollision(collisionCheckEntity, v.payload) ) {
					collisionCheckEntity.behavior.onCollision(currentTime, collisionCheckEntity, v.payload);
					// If v is moving, this will be called by the outer loop
					if( !v.payload.isMoving() ) v.payload.behavior.onCollision(currentTime, v.payload, collisionCheckEntity);
				}
			}
		};
		
		/**
		 * Call after changing an entity's coordinates but before updating its bounding box.
		 */
		private void updateEntityBounds(Entity e) {
			solidIndex.remove(e.boundingBox);
			e.updateBoundingBox();
			solidIndex.add(e.boundingBox);
		}
		
		public void passTime( double duration, long targetTime ) {
			currentTime = targetTime;
			for( Entity e : movingEntities ) {
				e.updatePosition(duration);
				e.behavior.onMove( currentTime, e );
				updateEntityBounds( e );
			}
			for( Entity e : movingEntities ) {
				collisionCheckEntity = e;
				solidIndex.findIntersection( e.boundingBox, collisionCallback );
			}
		}
	}
	
	static class Entity
	{
		public final AABB boundingBox = new AABB(this);
		public Entity parent;
		public double x, y, z, spaceRadius, solidRadius;
		public double vx, vy, vz;
		public double ax, ay, az;
		public double mass;
		public Color color;
		public EntityBehavior behavior;
		
		protected boolean isMoving() {
			return vx != 0 || vy != 0 || vz != 0 || ax != 0 || ay != 0 || az != 0;
		}
		
		protected void updatePosition( double duration ) {
			double halfDurationSquared = duration*duration/2;
			x += vx*duration + ax*halfDurationSquared;
			y += vy*duration + ay*halfDurationSquared;
			z += vz*duration + az*halfDurationSquared;
			vx += ax*duration;
			vy += ay*duration;
			vz += az*duration;
		}
		
		public AABB updateBoundingBox() {
			double maxRadius = Math.max(spaceRadius, solidRadius);
			this.boundingBox.setCentered( x, y, z, maxRadius );
			return boundingBox;
		}
	}
	
	Space space = new Space();
	HashSet<Runnable> worldUpdateListeners = new HashSet<Runnable>();
	
	@Override
	protected void passTime(long targetTime) {
		space.passTime( (targetTime - currentTime)/1000.0, targetTime );
		currentTime = targetTime;
		for( Runnable r : worldUpdateListeners ) r.run();
	}
	
	@Override public long getNextAutomaticUpdateTime() {
		return currentTime + 50;
	}
	
	public static void main( String[] args ) throws Exception {
		final Simulator sim = new Simulator();
		for( int i=0; i<100; ++i ) {
			Entity e = new Entity();
			e.x = Math.random()*200-100;
			e.y = Math.random()*200;
			//e.z = Math.random()*200-100;
			e.vx = Math.random()*20-10;
			e.vy = Math.random()*200-10;
			e.ay = -100;
			e.solidRadius = 10;
			e.color = Color.WHITE;
			sim.space.addEntity( e );
			e.behavior = new EntityBehavior() {
				long lastCollision = Long.MIN_VALUE;
				@Override public void onMove(long time, Entity self) {
					if( self.y < 0 && self.vy < 0 ) {
						// System.err.println("Bounce! " + self.x);
						self.vy = -self.vy;
					}
					if( time - lastCollision > 1000 ) {
						self.color = Color.WHITE;
					}
				}
				@Override public void onCollision(long time, Entity self, Entity other) {
					self.color = Color.RED;
					lastCollision = time;
				}
			};
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
				for( Entity e : sim.space.movingEntities ) {
					g.setColor( e.color );
					g.fillOval( (int)(e.x-e.solidRadius) + getWidth()/2, getHeight()-(int)(e.y-e.solidRadius), (int)(e.solidRadius*2), (int)(e.solidRadius*2) );
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
		
		sim.worldUpdateListeners.add(new Runnable() {
			@Override public void run() {
				canv.repaint();
			}
		});
		
		RealTimeEventSource<Object> eventSource = new QueuelessRealTimeEventSource<Object>();
		sim.currentTime = eventSource.getCurrentTime();
		EventLoop.run(eventSource, sim);
	}
}
