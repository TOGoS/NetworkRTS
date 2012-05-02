package togos.networkrts.experimental.gclient;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import togos.networkrts.awt.TimestampedPaintable;
import togos.networkrts.experimental.sim1.simulation.Simulator;

public class VectorCanvas extends Canvas
{
	private static final long serialVersionUID = 1L;
	
	long cwx = 0, cwy = 0;
	double zoom = 1.0, rotation = 0.0;
	
	Simulator sim = new Simulator();
	
	public VectorCanvas() {
	}
	
	TimestampedPaintable paintable = new TriTerrainRegionAWTRenderer();
	
	volatile BufferedImage buffer;
	
	class Repainter extends Thread {
		long repaintInterval;
		
		public Repainter( long repaintInterval ) {
			this.repaintInterval = repaintInterval;
		}
		
		protected BufferedImage getBuffer() {
			if( buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight() ) {
				buffer = getGraphicsConfiguration().createCompatibleImage( getWidth(), getHeight() );
			}
			return buffer;
		}
		
		public void run() {
			while( !Thread.interrupted() ) { 
				BufferedImage buffer = getBuffer();
				Graphics g = buffer.getGraphics();
				g.setClip( 0, 0, buffer.getWidth(), buffer.getHeight() );
				paintBuffer( g );
				repaint();
				try {
					Thread.sleep(repaintInterval);
				} catch( InterruptedException e ) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		};
	};
	
	Repainter repainter;
	
	public void paintBuffer( Graphics g ) {
		Graphics2D g2d = (Graphics2D)g;
		
		Rectangle clip = g.getClipBounds();
		
		g.setColor( Color.BLACK );
		g.fillRect( clip.x, clip.y, clip.width, clip.height );
		
		g2d.translate( getWidth() / 2, getHeight() / 2 );
		g2d.scale( zoom, zoom );
		g2d.rotate( rotation );
		
		paintable.paint( System.currentTimeMillis(), g2d );
	}
	
	public void paint( Graphics g ) {
		if( repainter == null ) {
			repainter = new Repainter(20);
			repainter.start();
		}
		BufferedImage img = buffer;
		if( img == null ) {
			g.setColor(Color.DARK_GRAY);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.LIGHT_GRAY);
			g.drawString("No backing buffer", 12, 12 );
		} else {
			g.drawImage( img, (getWidth() - img.getWidth()) / 2, (getHeight() - img.getHeight()) / 2, null );
		}
	}
	
	/* I still see background color when I resize the window.
	 * I think the parent component is drawing it, because overriding update
	 * does prevent it from filling the entire background each time. */
	public void update( Graphics g ) {
		paint(g);
	}
	
	public void setVisible( boolean vis ) {
		super.setVisible(vis);
		if( vis ) {

		} else {
			if( repainter != null ) {
				repainter.interrupt();
				repainter = null;
			}
		}
	}
	
	public static void main( String[] args ) {
		Frame f = new Frame("NetworkRTS VectorCanvas test");
		
		final VectorCanvas c = new VectorCanvas();
		c.setPreferredSize(new Dimension(640,480));
		c.setBackground(Color.ORANGE);
		
		f.add(c);
		f.pack();
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				e.getWindow().dispose();
			}
		});
		f.setVisible(true);
	}
}
