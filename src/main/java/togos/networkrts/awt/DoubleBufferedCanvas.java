package togos.networkrts.awt;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public abstract class DoubleBufferedCanvas extends Canvas
{
	private static final long serialVersionUID = 1L;
	
	BufferedImage buffer;
	
	protected BufferedImage getBuffer( int width, int height ) {
		if( buffer == null || buffer.getWidth() != width || buffer.getHeight() != height ) {
			buffer = getGraphicsConfiguration().createCompatibleImage(width, height);
		}
		return buffer;
	}
	
	protected void paintBackground( Graphics g ) {
		g.setColor( getBackground() );
		Rectangle clip = g.getClipBounds();
		g.fillRect( clip.x, clip.y, clip.width, clip.height );
	}
	
	protected abstract void _paint( Graphics g );
		
	@Override
	public void paint( Graphics g ) {
		int w = getWidth();
		int h = getHeight();
		
		if( w > 1024 || h > 1024 ) {
			// If the component is huge, only repaint the clipped region
			// (it might be reasonable to always do this)
			Rectangle clip = g.getClipBounds();
			
			Graphics2D bufferGraphics = getBuffer( clip.width, clip.height ).createGraphics();
			bufferGraphics.setClip( 0, 0, clip.width, clip.height );
			bufferGraphics.translate( -clip.x, -clip.y );
			_paint( bufferGraphics );
			g.drawImage( buffer, clip.x, clip.y, null );
		} else {
			Graphics2D bufferGraphics = getBuffer( w, h ).createGraphics();
			bufferGraphics.setClip( g.getClip() );
			_paint( bufferGraphics );
			g.drawImage( buffer, 0, 0, null );
		}
	}
	
	@Override
	public void update( Graphics g ) {
		paint(g);
	}
}
