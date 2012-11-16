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
import java.util.Random;


public class DungeonGame
{
	static class RegionCanvas extends Canvas {
		private static final long serialVersionUID = -6047879639768380415L;
		
		BlockField region;
		int cx, cy;
		
		@Override
		public void paint( Graphics g ) {
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
			/*
			g.setColor( Color.GREEN );
			g.drawOval( 0, 0, tileSize, tileSize );
			*/
		}
	}
	
	public static void main( String[] args ) {
		final CellCursor gs = new CellCursor();
		
		/*
		final Room region = new Room( 16, 16 );
		region.blockField.fill( Block.WALL.stack );
		gs.init( region, 0.5f, 0.5f );
		
		Random rand = new Random();
		int tx = 0, ty = 0;
		boolean gMode = false;
		for( int i=0; i<4096; ++i ) {
			if( gMode ) {
				for( int y=-5; y<=5; ++y ) {
					for( int x=-5; x<=5; ++x ) {
						region.blockField.setStack( tx+x, ty+y, Block.GRASS.stack );
					}
				}
				
				if( rand.nextBoolean() ) {
					tx += rand.nextInt(10)-5;
				} else {
					ty += rand.nextInt(10)-5;
				}
				
				if( rand.nextInt(10) <= 1 ) gMode = false;
			} else {
				int dx = 0, dy = 0;
				if( rand.nextBoolean() ) {
					dx = (rand.nextBoolean() ? 1 : -1);
				} else {
					dy += (rand.nextBoolean() ? 1 : -1);
				}
				
				for( int j=rand.nextInt(10); j>=0; --j ) {
					region.blockField.setStack( tx, ty, Block.FLOOR.stack );
					tx += dx; ty += dy;					
				}
				
				if( rand.nextInt(50) <= 1 ) gMode = true;
			}
		}
		*/
		
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
		db.west();
		db.west();
		
		gs.init( r0, 2.5f, 2.5f );
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
				gs.removeBlock( Block.PLAYER );
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): gs.move( 0, -1 ); break;
				case( KeyEvent.VK_DOWN  ): gs.move( 0, +1 ); break;
				case( KeyEvent.VK_LEFT  ): gs.move( -1, 0 ); break;
				case( KeyEvent.VK_RIGHT ): gs.move( +1, 0 ); break;
				default:
					System.err.println(kevt.getKeyCode());
				}
				gs.addBlock( Block.PLAYER );
				Raycast.project( gs.room, gs.x, gs.y, projection, projection.w/2, projection.h/2 );
				regionCanvas.cx = projection.w/2;
				regionCanvas.cy = projection.h/2;
				regionCanvas.repaint();
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
