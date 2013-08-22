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
		
		VolatileImage buffer;
		
		protected void paintBuffer( Graphics g ) {
			if( region == null ) return;
			
			int tileSize = 16;
			for( int y=0; y<region.h; ++y ) {
				for( int x=0; x<region.w; ++x ) {
					int highestOpaqueLayer = region.d-1;
					findOpaque: for( int z=region.d-1; z>=0; --z ) {
						Block[] stack = region.getStack( x, y, z );
						for( Block b : stack ) {
							if( b.opacity == 1 ) break findOpaque;
						}
						--highestOpaqueLayer; 
					}
					for( int z=highestOpaqueLayer; z<region.d; ++z ) {
						Block[] stack = region.getStack( x, y, z );
						for( Block b : stack ) {
							g.setColor(b.color);
							g.fillRect( (int)(getWidth()/2f + (x-cx) * tileSize), (int)(getHeight()/2f + (y-cy) * tileSize), tileSize, tileSize );
						}
					}
				}
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
	
	static class WalkingCharacter extends CellCursor {
		public int facingX = 0, facingY = 0;
		public int walkingX = 0, walkingY = 0;
		public long walkReadyTime = 0;
		public long walkStepInterval = 100;
		
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
	
	static class Command {
		public int walkX, walkY;
	}
	
	static class Simulator implements AutoEventUpdatable<Command> {
		WalkingCharacter player;
		long currentTime = 0;
		long nextAutoUpdateTime = 0;
		Trigger<Impulse> updated = new Trigger<Impulse>();
		
		final CellCursor tempCursor = new CellCursor();
		
		protected boolean attemptMove( WalkingCharacter p, int dx, int dy, int dz ) {
			tempCursor.set(player);
			tempCursor.move( dx, dy, dz );
			boolean blocked = false;
			for( Block b : tempCursor.getAStack() ) {
				if( b.blocking ) blocked = true;
			}
			if( !blocked ) {
				player.removeBlock( Block.PLAYER );
				player.set(tempCursor);
				player.addBlock(Block.PLAYER);
				return true;
			} else {
				return false;
			}
		}
		
		public synchronized void walkPlayer() {
			boolean movedX = false, movedY = false;
			boolean blockedX = false;
			if( player.walkingX != 0 ) {
				movedX = attemptMove( player, player.walkingX, 0, 0 );
				blockedX = !movedX;
			}
			if( player.walkingY != 0 ) {
				movedY = attemptMove( player, 0, player.walkingY, 0 );
			}
			if( blockedX && movedY ) {
				// Then try moving X-wise again!
				movedX = attemptMove( player, player.walkingX, 0, 0 );
			}
			if( movedX || movedY ) {
				player.walkReadyTime = currentTime + player.walkStepInterval;
				nextAutoUpdateTime = Math.min(nextAutoUpdateTime, player.walkReadyTime);
				this.updated.set(Impulse.INSTANCE);
			}
		}
		
		@Override public long getNextAutomaticUpdateTime() {
			return nextAutoUpdateTime;
		}
		
		/**
		 * Adjust all timestamps to make 'time' the current time
		 * without any time actually passing in the simulation.
		 */
		public void skipToTime( long time ) {
			player.walkReadyTime += (time - currentTime);
			nextAutoUpdateTime += (time - currentTime);
			currentTime = time;
		}
		
		@Override
		public AutoEventUpdatable<Command> update( long time, Command evt ) throws Exception {
			// Only works as long as there's only one active entity:
			if( time >= nextAutoUpdateTime ) {
				nextAutoUpdateTime = AutoEventUpdatable.TIME_INFINITY;
			}
			
			currentTime = time;
			if( evt != null ) {
				player.walkingX = evt.walkX;
				player.walkingY = evt.walkY;
			}
			if( time >= player.walkReadyTime ) {
				walkPlayer();
			}
			return this;
		}
	}
	
	public static void main( String[] args ) {
		Block[][] tileMap = new Block[][] { Block.EMPTY_STACK, Block.WALL.stack, Block.GRATING.stack, Block.FLOOR.stack };
		
		int[][] shapes = new int[][] {
			new int[] {
				3, 3, 3, 3, 3, 3,
				3, 3, 3, 3, 3, 3,
				3, 3, 3, 3, 3, 3,
				3, 3, 3, 3, 3, 3,
				3, 3, 3, 3, 3, 3,
				3, 3, 3, 3, 3, 3,
				
				1, 1, 1, 1, 1, 1,
				0, 0, 0, 0, 0, 1,
				0, 0, 0, 0, 0, 1,
				1, 0, 0, 0, 0, 0,
				1, 0, 0, 0, 0, 0,
				1, 1, 1, 1, 1, 1,
				
				1, 1, 1, 1, 1, 1,
				0, 0, 0, 0, 0, 1,
				0, 0, 0, 0, 0, 1,
				1, 0, 0, 0, 0, 0,
				1, 0, 0, 0, 0, 0,
				1, 1, 1, 1, 1, 1,
				
				1, 0, 0, 0, 0, 1,
				0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0,
				1, 2, 2, 0, 0, 0,
				1, 2, 2, 0, 0, 0,
				1, 1, 1, 1, 0, 1,
			},
		};
		
		Room r0 = new Room(6, 6, 4, Block.EMPTY_STACK);
		for( int i=0; i<shapes[0].length; ++i ) {
			r0.blockField.blockStacks[i] = tileMap[shapes[0][i]];
		}
		
		r0.neighbors.add(new Neighbor(r0, 6, 2, 0));
		r0.neighbors.add(new Neighbor(r0, -6, -2, 0));
		
		final WalkingCharacter player = new WalkingCharacter();
		player.set( r0, 2.51f, 2.51f, 2.51f );
		player.addBlock( Block.PLAYER );
		
		final Simulator sim = new Simulator();
		sim.player = player;
		
		final QueuelessRealTimeEventSource<Command> evtReg = new QueuelessRealTimeEventSource<Command>();
		
		final ViewManager vm = new ViewManager(64, 64, 8);
		final RegionCanvas regionCanvas = new RegionCanvas();
		regionCanvas.setPreferredSize( new Dimension(640,480) );
		regionCanvas.setBackground( Color.BLACK );
		regionCanvas.addKeyListener( new KeyListener() {
			boolean walkLeft  = false;
			boolean walkRight = false;
			boolean walkUp    = false;
			boolean walkDown  = false;
			
			protected void updateWalking() {
				Command cmd = new Command();
				cmd.walkX = (walkLeft && !walkRight) ? -1 : (walkRight && !walkLeft) ? 1 : 0;
				cmd.walkY = (walkUp   && !walkDown ) ? -1 : (walkDown  && !walkUp  ) ? 1 : 0;
				try {
					evtReg.post(cmd);
				} catch( InterruptedException e ) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			
			@Override public void keyPressed( KeyEvent kevt ) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): walkUp    = true; break;
				case( KeyEvent.VK_DOWN  ): walkDown  = true; break;
				case( KeyEvent.VK_LEFT  ): walkLeft  = true; break;
				case( KeyEvent.VK_RIGHT ): walkRight = true; break;
				default:
					System.err.println(kevt.getKeyCode());
				}
				updateWalking();
			}
			
			@Override public void keyReleased( KeyEvent kevt ) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): walkUp    = false; break;
				case( KeyEvent.VK_DOWN  ): walkDown  = false; break;
				case( KeyEvent.VK_LEFT  ): walkLeft  = false; break;
				case( KeyEvent.VK_RIGHT ): walkRight = false; break;
				}
				updateWalking();
			}
			
			@Override public void keyTyped( KeyEvent kevt ) {
			}
		});
		
		new Thread("Simulator") {
			public void run() {
				sim.skipToTime( evtReg.getCurrentTime() );
				try {
					EventLoop.run( evtReg, sim );
				} catch( InterruptedException e ) {
					Thread.currentThread().interrupt();
					return;
				} catch( Exception e ) {
					throw new RuntimeException(e);
				}
			}
		}.start();
		
		new Thread("Player view projector") {
			public void run() {
				while( true ) {
					try {
						sim.updated.waitAndReset();
					} catch( InterruptedException e ) {
						Thread.currentThread().interrupt();
						return;
					}
					vm.projectFrom(player);
					vm.updateCanvas(regionCanvas);
				}
			}
		}.start();
		
		sim.updated.set(Impulse.INSTANCE);
		
		final Frame window = new Frame("Robot Client");
		window.add(regionCanvas);
		window.pack();
		window.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				System.exit(0);
			}
		});
		window.setVisible(true);
		regionCanvas.requestFocus();
	}
}
