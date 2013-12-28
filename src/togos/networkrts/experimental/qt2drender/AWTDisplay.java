package togos.networkrts.experimental.qt2drender;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class AWTDisplay implements Display
{
	protected final Rectangle[] clipStack;
	public AWTDisplay( int clipStackSize ) {
		clipStack = new Rectangle[clipStackSize];
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
	
	@Override public void draw(ImageHandle ih, float x, float y, float w, float h) {
		// TODO: Round more better
		int intWidth = (int)w;
		int intHeight = (int)h;
		BufferedImage bImg = ih.optimized(intWidth, intHeight);
		g.drawImage(bImg, (int)x, (int)y, (int)x+intWidth, (int)y+intHeight, 0, 0, bImg.getWidth(), bImg.getHeight(), null);
	}
	
	@Override public void clip(float x, float y, float w, float h) {
		g.clipRect((int)x, (int)y, (int)w, (int)h);
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
