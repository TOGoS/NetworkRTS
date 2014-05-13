package togos.networkrts.experimental.game19.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.util.ResourceContext;
import togos.networkrts.experimental.hdr64.HDR64Util;
import togos.networkrts.util.Getter;
import togos.networkrts.util.ResourceNotFound;

public class AWTSurface implements Surface
{
	private final Graphics g;
	protected final ResourceContext ctx;
	protected final Getter<BufferedImage> imageGetter;
	
	public AWTSurface( Graphics g, ResourceContext ctx ) {
		this.g = g;
		this.ctx = ctx;
		this.imageGetter = ctx.imageGetter;
	}
	
	private Rectangle clip;
	protected Rectangle getClip() {
		if( clip == null ) clip = g.getClipBounds();
		if( clip == null ) clip = new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
		return clip;
	}
	
	@Override public int getClipTop() { return getClip().y; }
	@Override public int getClipLeft() { return getClip().x; }
	@Override public int getClipRight() {
		Rectangle c = getClip();
		return c.x + c.width;
	}
	@Override public int getClipBottom() {
		Rectangle c = getClip();
		return c.y + c.height;
	}

	@Override public Surface intersectClip(int x, int y, int w, int h) {
		Graphics g2 = g.create();
		g2.clipRect(x, y, w, h);
		return new AWTSurface(g2, ctx);
	}
	
	@Override public void fillRect(int x, int y, int w, int h, long hdr64Color) {
		g.setColor(new Color(HDR64Util.hdrToInt(hdr64Color, 0)));
		g.fillRect(x, y, w, h);
	}
	
	@Override public void drawImage(int x, int y, int w, int h, String imageUrn) {
		ImageHandle ih = ctx.getImageHandle(imageUrn);
		if( ih.isCompletelyTransparent ) return;
		try {
			// TODO: Scale and place according to icon x, y, w, h, where
			// -0.5 = top/left edge of cell, +0.5 = bottom/right edge of cell
			g.drawImage( ih.getScaled(imageGetter,w,h), x, y, null );
		} catch( ResourceNotFound e ) {
			System.err.println("Couldn't load image "+ih.original.getUri());
			g.setColor( Color.PINK );
			g.fillRect( x+1, y+1, w-2, h-2 );
		}
	}
}
