package togos.networkrts.experimental.qt2drender;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import togos.networkrts.util.Getter;
import togos.networkrts.util.ResourceNotFound;

public class AWTDisplay implements Display
{
	protected final Rectangle[] clipStack;
	protected final Getter<BufferedImage> imageSource;
	public AWTDisplay( int clipStackSize, Getter<BufferedImage> imageSource ) {
		clipStack = new Rectangle[clipStackSize];
		this.imageSource = imageSource;
		for( int i=0; i<clipStackSize; ++i ) clipStack[i] = new Rectangle();
	}
	
	protected int clipIndex;
	protected Graphics g;
	protected int width, height;
	public void init( Graphics g, int width, int height ) {
		this.g = g; this.width = width; this.height = height;
		resetClip();
	}
	
	@Override public void resetClip() {
		clipIndex = 0;
		g.setClip(0, 0, width, height);
	}
	
	@Override public void draw(ImageHandle img, float x, float y, float w, float h)
		throws ResourceNotFound 
	{
		if( img.isCompletelyTransparent(imageSource) ) return;
		int iw = (int)Math.ceil(w);
		int ih = (int)Math.ceil(h);
		BufferedImage bImg = img.optimized(imageSource, iw, ih);
		g.drawImage(bImg, (int)x, (int)y, (int)x+iw, (int)y+ih, 0, 0, bImg.getWidth(), bImg.getHeight(), null);
	}
	
	@Override public void clip(float x, float y, float w, float h) {
		int iw = (int)Math.ceil((x+w)-(int)x);
		int ih = (int)Math.ceil((y+h)-(int)y);
		g.clipRect((int)x, (int)y, iw, ih);
	}
	
	@Override public void saveClip() {
		g.getClipBounds(clipStack[clipIndex++]);
	}
	
	@Override public void restoreClip() {
		g.setClip(clipStack[--clipIndex]);
	}
	
	@Override public boolean hitClip(float x, float y, float w, float h ) {
		return g.hitClip( (int)x, (int)y, (int)w, (int)h );
	}
}
