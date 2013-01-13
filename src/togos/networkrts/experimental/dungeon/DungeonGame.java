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
		int cx, cy;
		
		VolatileImage buffer;
		
		protected void paintBuffer( Graphics g ) {
			int tileSize = 16;
			for( int y=0; y<region.h; ++y ) {
				for( int x=0; x<region.w; ++x ) {
					Block[] stack = region.getStack( x, y );
					if( stack == null ) continue;
					for( Block b : stack ) {
						g.setColor(b.color);
						g.fillRect( getWidth()/2 + (x-cx) * tileSize, getHeight()/2 + (y-cy) * tileSize, tileSize, tileSize );
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
	
	public static void main( String[] args ) {
		final CellCursor gs = new CellCursor();
		final CellCursor tempCursor = new CellCursor();
				
		Block[][] tileMap = new Block[][] { Block.FLOOR.stack, Block.WALL.stack };
		
		int[][] shapes = new int[][] {
			new int[] {
				1, 1, 1, 1, 1, 1,
				1, 0, 0, 0, 0, 1,
				1, 0, 0, 0, 0, 1,
				1, 0, 0, 0, 0, 1,
				1, 0, 0, 0, 0, 1,
				1, 1, 1, 1, 1, 1,
			},
		};
		
		DungeonBuilder db = new DungeonBuilder();
		Room r0 = db.makeRoom( 6, 6, tileMap, shapes[0] );
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
		
		gs.set( r0, 2.5f, 2.5f );
		gs.addBlock( Block.PLAYER );
		
		final BlockField projection = new BlockField( 55, 55 );
		Raycast.project( gs.room, gs.x, gs.y, projection, projection.w/2, projection.h/2 );
		
		final RegionCanvas regionCanvas = new RegionCanvas();
		regionCanvas.region = projection;
		regionCanvas.cx = projection.w/2;
		regionCanvas.cy = projection.h/2;
		
		regionCanvas.setPreferredSize( new Dimension(640,480) );
		regionCanvas.setBackground( Color.BLACK );
		regionCanvas.addKeyListener( new KeyListener() {
			@Override public void keyPressed( KeyEvent kevt ) {
				tempCursor.set(gs);
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): tempCursor.move( 0, -1 ); break;
				case( KeyEvent.VK_DOWN  ): tempCursor.move( 0, +1 ); break;
				case( KeyEvent.VK_LEFT  ): tempCursor.move( -1, 0 ); break;
				case( KeyEvent.VK_RIGHT ): tempCursor.move( +1, 0 ); break;
				default:
					System.err.println(kevt.getKeyCode());
				}
				boolean blocked = false;
				for( Block b : tempCursor.getAStack() ) {
					if( b.blocking ) blocked = true;
				}
				if( !blocked ) {
					gs.removeBlock( Block.PLAYER );
					gs.set(tempCursor);
					gs.addBlock( Block.PLAYER );
					Raycast.project( gs.room, gs.x, gs.y, projection, projection.w/2, projection.h/2 );
					regionCanvas.cx = projection.w/2;
					regionCanvas.cy = projection.h/2;
					regionCanvas.repaint();
				}
			}
			@Override public void keyReleased( KeyEvent kevt ) {
			}
			@Override public void keyTyped( KeyEvent kevt ) {
			}
		});
		
		final Frame window = new Frame("Robot Client");
		window.add(regionCanvas);
		window.pack();
		window.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				window.dispose();
			}
		});
		window.setVisible(true);
		regionCanvas.requestFocus();
	}
}
