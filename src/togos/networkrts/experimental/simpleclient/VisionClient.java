package togos.networkrts.experimental.simpleclient;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class VisionClient extends Applet
{
	private static final long serialVersionUID = 1L;

	VisibleWorldState vws = VisibleWorldState.BLANK;
	
	// centerX/Y = what coordinates in the 'world' correspond to the center of the screen?
	protected double centerX, centerY, zoom = 32;
	
	@Override
	public void init() {
		setBackground( Color.BLACK );
	}
	
	public void setState( VisibleWorldState vws ) {
		this.vws = vws;
		repaint();
	}
	
	@Override
	public void paint( Graphics g ) {
		int screenWidth = getWidth();
		int screenHeight = getHeight();
		int screenCenterX = screenWidth/2;
		int screenCenterY = screenHeight/2;
		
		g.setColor( Color.BLACK );
		g.fillRect( 0, 0, screenWidth, screenHeight );
		
		for( BackgroundArea area : vws.visibleBackgroundAreas ) {
			double minX = (area.minX - centerX) * zoom + screenCenterX;
			double minY = (area.minY - centerY) * zoom + screenCenterY;
			double maxX = (area.maxX - centerX) * zoom + screenCenterX;
			double maxY = (area.maxY - centerY) * zoom + screenCenterY;
			
			if( maxX < 0 || minX > screenWidth ) continue;
			if( maxY < 0 || minY > screenHeight ) continue;
			
			if( minX < -32 ) minX = -32;
			if( minY < -32 ) minY = -32;
			if( maxX > screenWidth + 32 ) maxX = screenWidth + 32;
			if( maxY > screenHeight + 32 ) maxY = screenHeight + 32;
			
			g.setColor( area.type.getColor() );
			g.fillRect( (int)minX, (int)minY, (int)(maxX-minX), (int)(maxY-minY) );
		}
	}
	
	public void update( Graphics g ) {
		paint( g );
	}
	
	public static void main( String[] args ) {
		final Frame f = new Frame("VisionClient");
		final VisionClient vc = new VisionClient();
		vc.setPreferredSize(new Dimension(640,480));
		f.add(vc);
		f.pack();
		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				f.dispose();
			}
		});
		f.setVisible(true);
	}
}
