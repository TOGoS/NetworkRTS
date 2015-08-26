package togos.networkrts.experimental.forthbots;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import togos.networkrts.experimental.forthbots.ForthBotsWorld.ForthVM;
import togos.networkrts.experimental.forthbots.ForthBotsWorld.Tile;
import togos.networkrts.ui.ImageCanvas;

public class ForthBotsDisplay {
	static enum TilePattern {
		SOLID0
	}
	
	static class TileIcon {
		static final int PAT_SOLID0 = 0;
		
		public final TilePattern pattern;
		public final Color color0;
		public final Color color1;
		public TileIcon( TilePattern pattern, Color color0, Color color1 ) {
			this.pattern = pattern;
			this.color0 = color0;
			this.color1 = color1;
		}
		public TileIcon( TilePattern pattern, Color color0 ) {
			this(pattern, color0, Color.MAGENTA);
		}
	}
	
	static class TileGrid {
		public final TileIcon[] tileIcons;
		public final int width;
		public final int height;
		public final byte[] tiles;
		
		public TileGrid(int width, int height) {
			this.width = width;
			this.height = height;
			tileIcons = new TileIcon[256];
			tileIcons[0] = new TileIcon(TilePattern.SOLID0, Color.BLACK, Color.BLACK);
			for( int i=1; i<255; ++i ) {
				tileIcons[i] = tileIcons[0];
			}
			this.tiles = new byte[width*height];
		}
	}
	
	public static void draw( TileGrid tg, int x, int y, int scale, Graphics g ) {
		Rectangle clipBounds = g.getClipBounds();
		for( int row=0; row<tg.height; ++row ) {
			if( y+(row+1)*scale <= clipBounds.y ) continue;
			if( y+row*scale >= clipBounds.y+clipBounds.height ) continue;
			for( int col=0; col<tg.width; ++col ) {
				if( x+(col+1)*scale <= clipBounds.x ) continue;
				if( x+col*scale >= clipBounds.x+clipBounds.width ) continue;
				
				TileIcon icon = tg.tileIcons[tg.tiles[row*tg.width+col]];
				g.setColor(icon.color0);
				g.fillRect(x+col*scale, y+row*scale, scale, scale);
			}
		}
	}
	
	public static void main( String[] args ) {
		final Frame f = new Frame("Forth Bots");
		ImageCanvas c = new ImageCanvas();
		c.setPreferredSize(new Dimension(512,384));
		f.add(c);
		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		f.pack();
		f.setVisible(true);
		
		ForthBotsWorld world = new ForthBotsWorld(64,64);
		
		Tile wall = new Tile();
		wall.iconNumber = 1;
		wall.flags |= Tile.FLAG_SOLID;
		
		short[] roboProgram = new short[4096];
		short pc = 0x400;
		roboProgram[ForthVM.PC_REG] = pc;
		roboProgram[ForthVM.DS_REG] = 0x300;
		roboProgram[ForthVM.PS_REG] = 0x400;
		roboProgram[pc++] = 0x0001;
		roboProgram[pc++] = ForthVM.MOVEMENT_REG;
		roboProgram[pc++] = ForthVM.I_PUT;
		roboProgram[pc++] = ForthVM.I_WAIT;
		roboProgram[pc++] = 0x400;
		roboProgram[pc++] = ForthVM.I_JUMP;
		ForthVM roboVm = new ForthVM(roboProgram);
		
		Tile robo = new Tile();
		robo.iconNumber = 2;
		robo.vm = roboVm;
		robo.flags |= Tile.FLAG_SOLID;
		
		world.tiles[0] = wall;
		world.tiles[1] = wall;
		world.tiles[2] = wall;
		world.tiles[64] = wall;
		world.tiles[128] = wall;
		world.tiles[131] = wall;
		world.tiles[66] = robo;
		
		BufferedImage canv = new BufferedImage(512,384,BufferedImage.TYPE_INT_ARGB);
		TileGrid tg = new TileGrid(world.width, world.height);
		tg.tileIcons[1] = new TileIcon(TilePattern.SOLID0, Color.GRAY);
		tg.tileIcons[2] = new TileIcon(TilePattern.SOLID0, Color.RED);
		while(true) {
			for( int i=0; i<world.tiles.length; ++i ) {
				tg.tiles[i] = (byte)world.tiles[i].iconNumber;
			}
			
			try {
				Graphics g = canv.getGraphics();
				g.setClip(0, 0, canv.getWidth(), canv.getHeight());
				draw(tg, 0, 0, 8, g);
				c.setImage(canv);
				
				world.step();
				Thread.sleep(100);
			} catch( InterruptedException e1 ) {
				return;
			}
		}
	}
}
