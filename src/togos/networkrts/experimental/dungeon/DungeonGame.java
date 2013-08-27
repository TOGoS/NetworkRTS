package togos.networkrts.experimental.dungeon;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import togos.networkrts.experimental.dungeon.Room.Neighbor;
import togos.networkrts.experimental.gensim.AutoEventUpdatable;
import togos.networkrts.experimental.gensim.EventLoop;
import togos.networkrts.experimental.gensim.QueuelessRealTimeEventSource;

public class DungeonGame
{
	/**
	 * Can be used to coordinate updates between threads,
	 * where later updates can override earlier ones that have
	 * not yet been processed.
	 */
	static class Trigger<T> {
		T value = null;
		public synchronized void set( T v ) {
			value = v;
			notifyAll();
		}
		public synchronized T waitAndReset() throws InterruptedException {
			while( value == null ) wait();
			T v = value;
			value = null;
			return v;
		}
	}
	
	static final class Impulse {
		private Impulse() { }
		public static final Impulse INSTANCE = new Impulse();
	}
	
	static class RegionCanvas extends Canvas {
		private static final long serialVersionUID = -6047879639768380415L;
		
		// TODO: I'd rather the canvas be dumber and not need to know the
		// region or the offset.  Instead, have another thread render
		// the scene and the canvas just blit it.
		BlockField region;
		float cx, cy;
		
		BlockFieldRenderer renderer = new BlockFieldRenderer();
		VolatileImage buffer;
		
		protected void paintBuffer( Graphics g ) {
			if( region == null ) return;
			
			synchronized(region) {
				renderer.render(region, cx, cy, g, 0, 0, getWidth(), getHeight());
			}
		}
		
		@Override
		public void paint( Graphics g ) {
			if( buffer == null || buffer.contentsLost() || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight() ) {
				buffer = createVolatileImage(getWidth(), getHeight());
			}
			if( buffer == null ) return; // *shrug*
			
			Graphics bg = buffer.getGraphics();
			bg.setClip(g.getClip());
			bg.setColor( getBackground() );
			bg.fillRect(0, 0, getWidth(), getHeight());
			paintBuffer( bg );
			g.drawImage( buffer, 0, 0, null );
		}
		
		@Override
		public void update( Graphics g ) {
			paint(g);
		}
	}
	
	static class WalkingCharacter extends CellCursor implements AutoEventUpdatable<WalkCommand> {
		public int facingX = 0, facingY = 0;
		public int walkingX = 0, walkingY = 0;
		public long walkReadyTime = 0;
		public long walkStepInterval = 100; // Interval between steps
		public long blockDelay = 10; // Delay after being blocked
		public Block block;
		
		public WalkingCharacter( Block block ) {
			this.block = block;
		}
		
		public void putAt( Room r, float x, float y, float z) {
			set(r, x, y, z);
			addBlock(block);
		}
		
		public void startWalking(int x, int y, int z) {
			this.facingX = x;
			this.facingY = y;
			this.walkingX = x;
			this.walkingY = y;
		}
		
		public void stopWalking() {
			this.walkingX = 0;
			this.walkingY = 0;
		}

		@Override public long getNextAutomaticUpdateTime() {
			if( walkingX == 0 && walkingY == 0 ) {
				return TIME_INFINITY;
			}
			return walkReadyTime;
		}

		@Override public AutoEventUpdatable<WalkCommand> update(long time, WalkCommand evt) throws Exception {
			startWalking( evt.walkX, evt.walkY, 0 );
			return this;
		}
	}
	
	static class ViewManager {
		BlockField projection;
		public ViewManager( int w, int h, int d ) {
			projection = new BlockField( w, h, d, Block.EMPTY_STACK );
		}
		
		float offX, offY;
		
		public void projectFrom( Room r, float x, float y, float z ) {
			Raycast.raycastXY( r, x, y, (int)z, projection, projection.w/2, projection.h/2 );
			this.offX = x - (int)x;
			this.offY = y - (int)y;
		}
		
		public void updateCanvas(RegionCanvas regionCanvas) {
			regionCanvas.region = projection;
			regionCanvas.cx = projection.w/2 + offX;
			regionCanvas.cy = projection.h/2 + offY;
			regionCanvas.repaint();
		}
		
		public void projectFrom(CellCursor pos) {
			projectFrom(pos.room, pos.x, pos.y, (int)pos.z);
		}
	}
	
	static class WalkCommand {
		public int walkX, walkY;
	}
	
	static class Simulator implements AutoEventUpdatable<WalkCommand> {
		WalkingCharacter commandee;
		List<WalkingCharacter> characters = new ArrayList<WalkingCharacter>();
		long currentTime = 0;
		Trigger<Impulse> updated = new Trigger<Impulse>();
		
		final CellCursor tempCursor = new CellCursor();
		
		protected boolean attemptMove( WalkingCharacter c, int dx, int dy, int dz ) {
			tempCursor.set(c);
			tempCursor.move( dx, dy, dz );
			boolean blocked = false;
			for( Block b : tempCursor.getAStack() ) {
				if( b.blocking ) blocked = true;
			}
			if( !blocked ) {
				c.removeBlock(c.block);
				c.set(tempCursor);
				c.addBlock(c.block);
				return true;
			} else {
				return false;
			}
		}
		
		public synchronized void doCharacterPhysics( WalkingCharacter c ) {
			if( c.walkReadyTime > currentTime ) return;
			
			boolean movedX = false, movedY = false;
			boolean blockedX = false;
			if( c.walkingX != 0 ) {
				movedX = attemptMove( c, c.walkingX, 0, 0 );
				blockedX = !movedX;
			}
			if( c.walkingY != 0 ) {
				movedY = attemptMove( c, 0, c.walkingY, 0 );
			}
			if( blockedX && movedY ) {
				// Then try moving X-wise again!
				movedX = attemptMove( c, c.walkingX, 0, 0 );
			}
			if( movedX || movedY ) {
				c.walkReadyTime = currentTime + c.walkStepInterval;
				this.updated.set(Impulse.INSTANCE);
			} else {
				c.walkReadyTime = currentTime + c.blockDelay;
			}
		}
		
		@Override public long getNextAutomaticUpdateTime() {
			long nextAutoUpdateTime = TIME_INFINITY;
			for( AutoEventUpdatable<?> c : characters ) {
				nextAutoUpdateTime = Math.min(nextAutoUpdateTime, c.getNextAutomaticUpdateTime());
			}
			if( nextAutoUpdateTime <= currentTime ) {
				nextAutoUpdateTime = currentTime + 1;
			}
			return nextAutoUpdateTime;
		}
		
		@Override
		public AutoEventUpdatable<WalkCommand> update( long time, WalkCommand evt ) throws Exception {
			currentTime = time;
			if( evt != null ) {
				commandee.update(time, evt);
			}
			for( WalkingCharacter c : characters ) {
				doCharacterPhysics( c );
			}
			return this;
		}
	}
	
	public static Simulator initSim( long initialTime ) {
		Room r0 = new Room(64, 64, 4, Block.EMPTY_STACK);
		for( int y=r0.getHeight()-1; y>=0; --y )
		for( int x=r0.getWidth()-1; x>=0; --x ) {
			r0.blockField.setStack(x, y, 0, Block.GRASS.stack);
		}
		
		CellCursor c = new CellCursor();
		CellCursor c1 = new CellCursor();
		Random rand = new Random();
		for( int i=0; i<100; ++i ) {
			c.set(r0, rand.nextInt(64), rand.nextInt(64), 1);
			int r = rand.nextInt(3);
			for( int dy=0; dy<=1+r; ++dy )
			for( int dx=0; dx<=1+r; ++dx ) {
				c1.set(c);
				c1.move(dx, dy, 0);
				c1.setStack( Block.FOLIAGE.stack );
			}
		}
		for( int i=0; i<20; ++i ) {
			c.set(r0, rand.nextInt(64), rand.nextInt(64), 1);
			for( int dy=-1; dy<=1; ++dy )
			for( int dx=-1; dx<=1; ++dx ) {
				c1.set(c);
				c1.move(dx, dy, 0);
				c1.setStack( Block.WALL.stack );
			}
		}
		
		r0.neighbors.add(new Neighbor(r0,  64,  32, 0));
		r0.neighbors.add(new Neighbor(r0, -64, -32, 0));
		r0.neighbors.add(new Neighbor(r0,  64, -32, 0));
		r0.neighbors.add(new Neighbor(r0, -64,  32, 0));
		r0.neighbors.add(new Neighbor(r0,   0, -64, 0));
		r0.neighbors.add(new Neighbor(r0,   0,  64, 0));
		
		final WalkingCharacter player = new WalkingCharacter( Block.PLAYER );
		player.walkReadyTime = initialTime;
		player.putAt( r0, 2.51f, 2.51f, 1.51f );
		
		final Simulator sim = new Simulator();
		sim.commandee = player;
		sim.characters.add(player);
		
		final WalkingCharacter bot = new WalkingCharacter( Block.BOT );
		bot.putAt( r0, 3.51f, 3.51f, 1.51f);
		bot.startWalking( 1, 1, 0);
		sim.characters.add(bot);
		
		final WalkingCharacter bot2 = new WalkingCharacter( Block.BOT );
		bot2.putAt( r0, 4.51f, 3.51f, 1.51f);
		bot2.startWalking( 1, 1, 0);
		sim.characters.add(bot2);
		
		final WalkingCharacter bot3 = new WalkingCharacter( Block.BOT );
		bot3.putAt( r0, 1.51f, 3.51f, 1.51f);
		bot3.startWalking( 1, 1, 0);
		sim.characters.add(bot3);

		return sim;
	}
}
