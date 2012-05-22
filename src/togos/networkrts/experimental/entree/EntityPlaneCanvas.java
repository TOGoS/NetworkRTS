package togos.networkrts.experimental.entree;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import togos.networkrts.awt.DoubleBufferedCanvas;

class EntityPlaneCanvas extends DoubleBufferedCanvas
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
	
	public void paintEntityLayer( final int layer, final long timestamp, final Graphics g ) {
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
					scale, timestamp, layer
				);
			}
		} );
	}
	
	@Override
	public void _paint( final Graphics g ) {
		long timestamp = System.currentTimeMillis();
		paintBackground(g);
		paintEntityLayer( 0, timestamp, g );
		paintEntityLayer( 1, timestamp, g );
		paintEntityLayer( 2, timestamp, g );
		paintEntityLayer( 3, timestamp, g );
		paintEntityLayer( 4, timestamp, g );
		paintEntityLayer( 5, timestamp, g );
		paintEntityLayer( 6, timestamp, g );
		paintEntityLayer( 7, timestamp, g );
	}		
}