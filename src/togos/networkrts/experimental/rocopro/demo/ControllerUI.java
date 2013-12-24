package togos.networkrts.experimental.rocopro.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JPanel;

public class ControllerUI {
	static class Tile {
		final Color color;
		
		public Tile( Color c ) {
			this.color = c;
		}
	}
	
	class ViewCanvas extends JPanel {
		private static final long serialVersionUID = 1L;
		
		float gridOffsetX, gridOffsetY;
		int gridWidth = 0, gridHeight = 0;
		Tile[] gridTiles = new Tile[0];
		
		int scale = 32;
		
		synchronized Tile[] allocateGrid( int w, int h ) {
			if( w*h > gridTiles.length ) {
				gridTiles = new Tile[w*h];
			}
			return gridTiles;
		}
		
		public synchronized void gridUpdated( float offX, float offY, Tile[] tiles, int w, int h ) {
			this.gridOffsetX = offX;
			this.gridOffsetY = offY;
			this.gridWidth = w;
			this.gridHeight = h;
			this.gridTiles = tiles;
			repaint();
		}
		
		@Override public void paint( Graphics g ) {
			int screenOffsetX = (int)(getWidth()/2 + gridOffsetX*scale);
			int screenOffsetY = (int)(getHeight()/2 + gridOffsetY*scale);
			
			int w, h;
			Tile[] tiles;
			synchronized(this) {
				w = gridWidth;
				h = gridHeight;
				tiles = gridTiles;
			}
			
			for( int j=0, y=0; y<h; ++y ) for( int x=0; x<w; ++x, ++j ) {
				g.setColor(tiles[j].color);
				g.fillRect(screenOffsetX+x*scale, screenOffsetY+y*scale, scale, scale);
			}
		}
	}
	
	final ViewCanvas vc = new ViewCanvas();
	
	public synchronized void gridUpdated( float offX, float offY, Tile[] tiles, int w, int h ) {
		vc.gridUpdated(offX, offY, tiles, w, h);
	}
	
	public void show() {
		final Frame f = new Frame("Robot control protocol demo controller UI");
		vc.setPreferredSize(new Dimension(640, 480));
		vc.setBackground(Color.BLACK);
		f.setBackground(Color.BLACK);
		f.add(vc);
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				f.dispose();
			}
		});
		f.pack();
		f.setVisible(true);
	}
	
	public static void main( String[] args ) {
		ControllerUI cui = new ControllerUI();
		Tile a = new Tile( Color.GRAY );
		Tile b = new Tile( Color.GREEN );
		
		cui.gridUpdated(-1.5f, -1.5f, new Tile[] {
			a, a, b,
			a, b, b,
			b, b, a
		}, 3, 3);
		cui.show();
	}
}
