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

public class DungeonGame
{
	static class RegionCanvas extends Canvas {
		private static final long serialVersionUID = -6047879639768380415L;
		
		BlockField region;
		float cx, cy;
		
		VolatileImage buffer;
		
		protected void paintBuffer( Graphics g ) {
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
	
	static class Player extends CellCursor {
		public int facingX = 0, facingY = 0;
		public int walkingX = 0, walkingY = 0;
		
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
			projection = new BlockField( w, h, d );
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
	
	static class Simulator {
		Player player;
		
		final CellCursor tempCursor = new CellCursor();
		
		public synchronized boolean tick( long cTime ) {
			boolean updated = false;
			if( player.walkingX != 0 || player.walkingY != 0 ) {
				tempCursor.set(player);
				tempCursor.move( player.walkingX, player.walkingY, 0 );
				boolean blocked = false;
				for( Block b : tempCursor.getAStack() ) {
					if( b.blocking ) blocked = true;
				}
				if( !blocked ) {
					player.removeBlock( Block.PLAYER );
					player.set(tempCursor);
					player.addBlock(Block.PLAYER);
					updated = true;
				}
			}
			return updated;
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
				1, 0, 0, 0, 0, 1,
				1, 0, 0, 0, 0, 1,
				1, 0, 0, 0, 0, 1,
				1, 0, 0, 0, 0, 1,
				1, 1, 1, 1, 1, 1,
				
				1, 1, 1, 1, 1, 1,
				1, 0, 0, 0, 0, 1,
				1, 0, 0, 0, 0, 1,
				1, 0, 0, 0, 0, 1,
				1, 0, 0, 0, 0, 1,
				1, 1, 1, 1, 1, 1,
				
				1, 2, 2, 2, 2, 1,
				2, 0, 0, 0, 0, 2,
				2, 0, 0, 0, 0, 2,
				2, 0, 0, 0, 0, 2,
				2, 0, 0, 0, 0, 2,
				1, 2, 2, 2, 2, 1,
			},
		};
		
		DungeonBuilder db = new DungeonBuilder();
		Room r0 = db.makeRoom( 6, 6, 4, tileMap, shapes[0] );
		db.north();
		db.north();
		db.east();
		db.south();
		Room r1 = db.west();
		db.west();
		db.north();
		db.north();
		db.westTo(r0);
		db.currentRoom = r0;
		db.east();
		db.east();
		db.north();
		db.currentRoom = r1;
		db.south();
		Room r2 = db.south();
		db.west();
		
		Random rand = new Random();
		int dir = 2;
		for( int i=0; i<50 || dir == 0; ++i ) {
			dir = NumUtil.tmod(dir + rand.nextInt(3) - 1, 4);
			switch( dir ) {
			case( 0 ): db.east(); break;
			case( 1 ): db.south(); break;
			case( 2 ): db.west(); break;
			case( 3 ): db.north(); break;
			}
		}
		db.eastTo(r2);
		
		final Player player = new Player();
		player.set( r0, 2.51f, 2.51f, 2.51f );
		player.addBlock( Block.PLAYER );

		final Simulator sim = new Simulator();
		sim.player = player;
		
		final ViewManager vm = new ViewManager(64, 64, 8);
		final RegionCanvas regionCanvas = new RegionCanvas();
		regionCanvas.setPreferredSize( new Dimension(640,480) );
		regionCanvas.setBackground( Color.BLACK );
		regionCanvas.addKeyListener( new KeyListener() {
			// TODO: Replace with nicer walking direction state management
			@Override public void keyPressed( KeyEvent kevt ) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): player.startWalking( 0, -1, 0 ); break;
				case( KeyEvent.VK_DOWN  ): player.startWalking( 0, +1, 0 ); break;
				case( KeyEvent.VK_LEFT  ): player.startWalking( -1, 0, 0 ); break;
				case( KeyEvent.VK_RIGHT ): player.startWalking( +1, 0, 0 ); break;
				default:
					System.err.println(kevt.getKeyCode());
				}
				if( sim.tick(System.currentTimeMillis()) ) {
					vm.projectFrom(player);
					vm.updateCanvas(regionCanvas);
				}
			}
			@Override public void keyReleased( KeyEvent kevt ) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): player.stopWalking(); break;
				case( KeyEvent.VK_DOWN  ): player.stopWalking(); break;
				case( KeyEvent.VK_LEFT  ): player.stopWalking(); break;
				case( KeyEvent.VK_RIGHT ): player.stopWalking(); break;
				}
			}
			@Override public void keyTyped( KeyEvent kevt ) {
			}
		});
		
		vm.projectFrom(player);
		vm.updateCanvas(regionCanvas);
		
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
		
		while( true ) {
			if( sim.tick(System.currentTimeMillis()) ) {
				vm.projectFrom(player);
				vm.updateCanvas(regionCanvas);
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e1) {
				System.exit(0);
			}
		}
	}
}
