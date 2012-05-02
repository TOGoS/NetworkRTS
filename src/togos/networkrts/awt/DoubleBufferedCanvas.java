package togos.networkrts.awt;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public abstract class DoubleBufferedCanvas extends Canvas
{
	private static final long serialVersionUID = 1L;
	
	BufferedImage buffer;
	
	protected BufferedImage getBuffer() {
		if( buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight() ) {
			buffer = getGraphicsConfiguration().createCompatibleImage(getWidth(), getHeight());
		}
		return buffer;
	}
	
	protected void paintBackground( Graphics2D g ) {
		g.setColor( getBackground() );
		g.fillRect( 0, 0, getWidth(), getHeight() );
	}
	
	protected abstract void _paint( Graphics2D g );
		
	@Override
	public void paint( Graphics g ) {
		Graphics2D bufferGraphics = getBuffer().createGraphics();
		bufferGraphics.setClip( g.getClip() );
		_paint( bufferGraphics );
		g.drawImage( buffer, 0, 0, null );
	}
	
	@Override
	public void update( Graphics g ) {
		paint(g);
	}
}
