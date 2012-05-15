package togos.networkrts.experimental.entree;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

class EntityPlaneCanvas extends Canvas
{
	private static final long serialVersionUID = 1L;
	
	protected EntityPlane<? extends AWTDrawableEntity> plane = new QuadTreeEntityPlane(128, 128, EntityQuadTreeNode.EMPTY, 128);
	protected double centerX = 0, centerY = 0, scale = 1.0;
	
	public void setState( EntityPlane<? extends AWTDrawableEntity> plane, double cx, double cy, double scale ) {
		this.plane = plane;
		this.centerX = cx;
		this.centerY = cy;
		this.scale = scale;
	}
	
	public void paintLayer( int layer, final Graphics g ) {
		final int w = getWidth();
		final int h = getHeight();
		Rectangle gClip = g.getClipBounds();
		ClipRectangle wClip = new ClipRectangle(
			centerX + (gClip.getMinX() - w/2) / scale,
			centerY + (gClip.getMinY() - h/2) / scale,
			gClip.getWidth() / scale,
			gClip.getHeight() / scale
		);
		plane.eachEntity( wClip, layer << 1, (~layer << 1) & (0x7 << 1), new Iterated() {
			@Override
			public void item( Object o ) {
				AWTDrawableEntity e = (AWTDrawableEntity)o;
				e.draw( (Graphics2D)g,
					(e.getX() - centerX)*scale + w/2,
					(e.getY() - centerY)*scale + h/2,
					scale
				);
			}
		} );
	}
	
	@Override
	public void paint( final Graphics g ) {
		paintLayer( 0, g );
		paintLayer( 1, g );
		paintLayer( 2, g );
		paintLayer( 3, g );
		paintLayer( 4, g );
		paintLayer( 5, g );
		paintLayer( 6, g );
		paintLayer( 7, g );
	}		
}