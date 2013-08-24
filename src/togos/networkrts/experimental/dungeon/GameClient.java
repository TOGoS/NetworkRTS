package togos.networkrts.experimental.dungeon;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class GameClient
{
	static class ImageCanvas extends Canvas
	{
		private static final long serialVersionUID = 1L;
		
		public ImageCanvas() {
			setImage(null);
		}
		
		BufferedImage image;
		public void setImage( BufferedImage img ) {
			this.image = img;
			setPreferredSize( img == null ? new Dimension(512,384) : new Dimension(img.getWidth(), img.getHeight()));
			repaint();
		}
		
		protected int offX, offY;
		protected float scale;
		
		// Return the image so that this.image can be safely
		// updated from another thread
		protected BufferedImage initOffsets() {
			BufferedImage img = image;
			
			if( img == null ) return img;
			
			scale = 1;
			while( img.getWidth() * scale > getWidth() || img.getHeight() * scale > getHeight() ) {
				scale /= 2;
			}
			while( img.getWidth() * scale * 2 <= getWidth() && img.getHeight() * 2 <= getHeight() ) {
				scale *= 2;
			}
			
			offX = (int)(getWidth() - img.getWidth() * scale) / 2;
			offY = (int)(getHeight() - img.getHeight() * scale) / 2;
			
			return img;
		}
		
		@Override public void paint( Graphics g ) {
			g.setColor(getBackground());
			BufferedImage img = initOffsets();
			if( img == null || scale == 0 ) {
				g.fillRect(0,0,getWidth(),getHeight());
			} else {
				int scaledImageWidth  = (int)(img.getWidth()  * scale);
				int scaledImageHeight = (int)(img.getHeight() * scale);
				int right = getWidth() - (offX + scaledImageWidth); 
				int bottom = getHeight() - (offY + scaledImageHeight);
				g.fillRect(    0, offY,             offX, scaledImageHeight);
				g.fillRect(right, offY, getWidth()-right, scaledImageHeight);
				g.fillRect(0,      0, getWidth(), offY);
				g.fillRect(0, bottom, getWidth(), getHeight()-bottom);
				g.drawImage( img, offX, offY, scaledImageWidth, scaledImageHeight, null );
			}
		}
		
		@Override public void update( Graphics g ) { paint(g); }
	}
	
	public static void main( String[] args ) {
		BufferedImage img = new BufferedImage( 768, 512, BufferedImage.TYPE_INT_ARGB );
		Graphics g = img.getGraphics();
		g.setColor(Color.DARK_GRAY);
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.setColor(Color.RED);
		g.drawString("HAewwwo", 4, 12);
		
		final ImageCanvas leCanv = new ImageCanvas();
		leCanv.setImage(img);
		leCanv.setBackground(Color.BLACK);
		
		final Frame win = new Frame("D6");
		win.add( leCanv );
		win.pack();
		win.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing( WindowEvent evt ) {
				System.exit(0);
			}
		});
		
		win.setVisible(true);
		
		
		// TODO: stuff!
		new Thread() {
			public void run() {
				
			};
		}.start();
	}
}
