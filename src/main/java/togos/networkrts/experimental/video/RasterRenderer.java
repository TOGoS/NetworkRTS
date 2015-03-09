package togos.networkrts.experimental.video;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

public class RasterRenderer
{
	public static final class RasterImage {
		public final int width, height;
		public final int[] data;
		public final boolean hasHoles;
		public final boolean hasAlpha;
		
		public RasterImage( int width, int height, int[] data ) {
			assert width >= 0;
			assert height >= 0;
			assert width * height == data.length;
			
			boolean hasHoles = false, hasAlpha = false;
			for( int i=0; i<data.length; ++i ) {
				switch( data[i] & 0xFF000000 ) {
				case( 0xFF000000 ): break;
				case( 0x00000000 ): hasHoles = true; break;
				default: hasAlpha = true; break;
				}
			}
			
			this.width = width;
			this.height = height;
			this.data = data;
			this.hasHoles = hasHoles;
			this.hasAlpha = hasAlpha;
		}
		
		public RasterImage( int width, int height ) {
			this(width, height, new int[width*height]);
		}
	}
	
	protected final int[] data;
	protected final int width;
	protected final int height;
	
	public RasterRenderer( int w, int h, int[] data ) {
		assert data.length == w * h;
		this.width  = w;
		this.height = h;
		this.data   = data;
	}
	
	public RasterRenderer( int w, int h ) {
		this( w, h, new int[w*h] );
	}
	
	protected final void drawImageNoBoundsChecks( int x, int y, RasterImage i ) {
		int sIdx = 0;
		for( int y1=0; y1<i.height; ++y1 ) {
			int dIdx = (y+y1) * width + x;
			for( int x1=0; x1<i.width; ++x1, ++dIdx, ++sIdx ) {
				data[dIdx] = i.data[sIdx];
			}
		}
	}
	
	public void fill( int color ) {
		for( int i=data.length-1; i>=0; --i ) data[i] = color;
	}
	
	public void drawImage( int x, int y, RasterImage i ) {
		if( x >=0 && y >= 0 && x+i.width <= width && y+i.height <= height ) {
			drawImageNoBoundsChecks( x, y, i );
			return;
		}
		
		if( x + i.width  <= 0 ) return;
		if( y + i.height <= 0 ) return;
		if( x >= width  ) return;
		if( y >= height ) return;
		
		int sx0 = 0, sy0 = 0;
		if( x < 0 ) { sx0 -= x; }
		if( y < 0 ) { sy0 -= y; }
		
		int sx2 = i.width, sy2 = i.height;
		if( x + sx2 >= width  ) sx2 = width  - x;
		if( y + sy2 >= height ) sy2 = height - y;
		
		for( int sy1=sy0; sy1<sy2; ++sy1 ) {
			for( int sx1=sx0; sx1<sx2; ++sx1 ) {
				data[(y+sy1)*width+x+sx1] = i.data[sy1*i.width+sx1];
			}
		}
	}
	
	public void drawLayer( int x, int y, RasterImage[] tileImages, byte[] cellData ) {
		
	}
	
	static class BufferCanvas extends Canvas {
		private static final long serialVersionUID = 1L;
		
		protected BufferedImage buf;
		protected int scale;
		
		public synchronized void setBuffer( BufferedImage buf, int scale ) {
			this.buf = buf;
			this.scale = scale;
			this.repaint();
		}
		
		@Override
		public void paint(Graphics g) {
			if( buf != null ) {
				g.drawImage(buf, 0, 0, scale*buf.getWidth(), scale*buf.getHeight(), null);
			}
		}
		
		@Override
		public void update(Graphics g) {
			paint(g);
		}
	}
	
	static final int fmod( int a, int b ) {
		return a < 0 ? (b + a % b) % b : a % b; 
	}
	
	public static void main(String[] args) {
		final Frame f = new Frame("RasterRenderer demo");
		final RasterRenderer rr = new RasterRenderer( 320, 240 );
		final BufferCanvas a = new BufferCanvas();
		a.setPreferredSize( new Dimension(640,480));
		f.add(a);
		
		f.pack();
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		f.setVisible(true);
		
		RasterImage dumb = new RasterImage( 32, 32 );
		dumb.data[0] = 0xFF0000FF;
		
		//rr.fill( 0xFF000000 );
		BufferedImage bi = new BufferedImage(rr.width, rr.height, BufferedImage.TYPE_INT_RGB);
		int x = 0;
		int y = 0;
		int z = 0;
		Random r = new Random();
		while( true ) {
			x += r.nextInt(3)-1;
			y += r.nextInt(3)-1;
			z += 1;
			x = fmod(x, rr.width );
			y = fmod(y, rr.height);
			rr.data[x + y*rr.width] = 0xFFFFFFFF;
			rr.drawImage( z, z/2, dumb );
			bi.setRGB( 0, 0, rr.width, rr.height, rr.data, 0, rr.width );
			a.setBuffer( bi, 2 );
			/*
			try {
				Thread.sleep(25);
			} catch( InterruptedException e ) {
				Thread.currentThread().interrupt();
				return;
			}
			*/
		}
	}
}
