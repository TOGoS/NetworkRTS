package togos.networkrts.experimental.raycast;

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

public class BlockCastDemo
{
	protected static int tmod( int num, int den ) {
		int rem = num % den;
		return rem < 0 ? den + rem : rem;
	}
	
	static class Block {
		public static final Block[] EMPTY_STACK = new Block[0];
		
		final float opacity;
		final Color color;
		
		public Block( Color c, float opacity ) {
			this.color = c;
			this.opacity = opacity;
		}
		
		public final Block[] stack = new Block[] { this };
		
		public static final Block FLOOR = new Block( Color.GRAY, 0 );
		public static final Block GRASS = new Block( Color.GREEN, 0 );
		public static final Block WALL = new Block( Color.WHITE, 1 );
		public static final Block PLAYER = new Block( Color.YELLOW, 0 );
	}
	
	static class Region {
		final int w, h;
		Block[][] blockStacks;
		
		public Region( int w, int h ) {
			this.w = w; this.h = h;
			this.blockStacks = new Block[w*h][];
		}
		
		public void fill( Block[] stack ) {
			for( int i=0; i<blockStacks.length; ++i ) {
				blockStacks[i] = stack;
			}
		}
		
		public void clear() { fill(null); }
		
		protected final int stackIndex( int x, int y ) {
			return w*tmod(y,h)+tmod(x,w);
		}
		
		public void setStack( int x, int y, Block[] stack ) {
			blockStacks[stackIndex(x,y)] = stack;
		}
		
		public Block[] getStack( int x, int y ) {
			return blockStacks[stackIndex(x,y)];
		}
		
		public void addBlock( int x, int y, Block b ) {
			int index = stackIndex(x,y);
			Block[] stack = blockStacks[index];
			if( stack == null || stack.length == 0 ) {
				blockStacks[index] = b.stack;
				return;
			}
			for( int j=0; j<stack.length; ++j ) {
				if( stack[j] == b ) return;
			}
			Block[] newStack = new Block[stack.length+1];
			for( int j=0; j<stack.length; ++j ) newStack[j] = stack[j];
			newStack[stack.length] = b;
			blockStacks[index] = newStack;
		}
		
		public void removeBlock( int x, int y, Block b ) {
			int index = stackIndex(x,y);
			Block[] stack = blockStacks[index];
			if( stack == null ) return;
			if( stack.length == 0 ) return;
			for( int j=0; j<stack.length; ++j ) {
				if( stack[j] == b ) {
					Block[] newStack;
					if( stack.length == 2 ) {
						newStack = stack[j^1].stack;
					} else {
						newStack = new Block[stack.length-1];
						for( int k=0; k<j; ++k ) newStack[j] = stack[k];
						for( int k=j+1; k<stack.length; ++k ) newStack[k-1] = stack[k];
					}
					blockStacks[index] = newStack;
				}
			}
		}
	}
	
	static class RegionCanvas extends Canvas {
		private static final long serialVersionUID = -6047879639768380415L;
		
		Region region;
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
	
	static void project( Region src, float x, float y, Region dest, int destX, int destY ) {
		dest.clear();
		
		final int rayCount=512;
		for( int i=0; i<rayCount; ++i ) {
			// 64 rays!
			float angle = i*(float)Math.PI*2/rayCount;
			float dx = (float)Math.cos(angle);
			float dy = (float)Math.sin(angle);
			float cx = x, cy = y;
			float visibility = 1;
			for( int j=100; j>=0 && visibility > 0; cx += dx, cy += dy, --j ) {
				int cellX = (int)Math.floor(cx), cellY = (int)Math.floor(cy);
				Block[] stack = src.getStack( cellX, cellY );
				if( stack == null ) break;
				
				int destCX = destX+cellX-(int)Math.floor(x);
				int destCY = destY+cellY-(int)Math.floor(y);
				
				if( destCX < 0 || destCX >= dest.w ) break;
				if( destCY < 0 || destCY >= dest.h ) break;
				
				dest.setStack( destCX, destCY, stack );
				for( Block b : stack ) visibility -= b.opacity;
			}
		}
	}
	
	static class GameState {
		int px, py;
	}
	
	public static void main( String[] args ) {
		final GameState gs = new GameState();
		
		final Region region = new Region( 256, 256 );
		region.fill( Block.WALL.stack );
		
		Random rand = new Random();
		int tx = 0, ty = 0;
		
		boolean gMode = false;
		
		for( int i=0; i<4096; ++i ) {
			if( gMode ) {
				for( int y=-5; y<=5; ++y ) {
					for( int x=-5; x<=5; ++x ) {
						region.setStack( tx+x, ty+y, Block.GRASS.stack );
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
					region.setStack( tx, ty, Block.FLOOR.stack );
					tx += dx; ty += dy;					
				}
				
				if( rand.nextInt(50) <= 1 ) gMode = true;
			}
		}
		region.addBlock( gs.px, gs.py, Block.PLAYER );
		
		final Region projection = new Region( 55, 55 );
		project( region, gs.px+0.5f, gs.py+0.5f, projection, projection.w/2, projection.h/2 );
		
		final RegionCanvas regionCanvas = new RegionCanvas();
		regionCanvas.region = projection;
		regionCanvas.cx = projection.w/2;
		regionCanvas.cy = projection.h/2;
		
		regionCanvas.setPreferredSize( new Dimension(640,480) );
		regionCanvas.setBackground( Color.BLACK );
		regionCanvas.addKeyListener( new KeyListener() {
			@Override public void keyPressed( KeyEvent kevt ) {
				region.removeBlock( gs.px, gs.py, Block.PLAYER );
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): gs.py -= 1; break;
				case( KeyEvent.VK_DOWN  ): gs.py += 1; break;
				case( KeyEvent.VK_LEFT  ): gs.px -= 1; break;
				case( KeyEvent.VK_RIGHT ): gs.px += 1; break;
				default:
					System.err.println(kevt.getKeyCode());
				}
				region.addBlock( gs.px, gs.py, Block.PLAYER );
				project( region, gs.px+0.5f, gs.py+0.5f, projection, projection.w/2, projection.h/2 );
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
