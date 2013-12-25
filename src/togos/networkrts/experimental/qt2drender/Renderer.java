package togos.networkrts.experimental.qt2drender;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * The goal is for this to be a general-purpose renderer
 * for 2D scenes with parallax backgrounds exposed through
 * portals
 */
public class Renderer
{
	public static class RenderNode {
		public static final RenderNode EMPTY = new RenderNode(null, 0, 0, 0, 0, null, null, null, null, null);
		
		final RenderNode background;
		/**
		 * Position of background node's top-left corner relative
		 * to this node's top-left corner
		 */
		final int backgroundX0, backgroundY0, backgroundSize;
		final int backgroundDistance;
		
		final ImageHandle image;
		final RenderNode n0, n1, n2, n3;
		
		public RenderNode( RenderNode background, int bgX, int bgY, int bgSize, int bgDistance, ImageHandle image, RenderNode n0, RenderNode n1, RenderNode n2, RenderNode n3 ) {
			this.background = background;
			this.backgroundX0 = bgX;
			this.backgroundY0 = bgY;
			this.backgroundSize = bgSize;
			this.backgroundDistance = bgDistance;
			this.image = image;
			this.n0 = n0;
			this.n1 = n1;
			this.n2 = n2;
			this.n3 = n3;
		}
	}
	
	/** 
	 * @param n node to draw
	 * @param x position to draw node on the screen if it were at distance=1
	 * @param y position to draw node on the screen if it were at distance=1
	 * @param nodeSize size to draw node if it were at distance=1
	 * @param distance
	 * @param g
	 * @param centerX
	 * @param centerY
	 */
	public void drawPortal( RenderNode n, float x, float y, float nodeSize, float distance, Graphics g, float scale, float centerX, float centerY ) {
		// clip to actual region on screen being drawn at
		
		int screenNodeSize = (int)Math.ceil((double)nodeSize*scale/distance);
		int screenX = (int)(centerX + (x*scale)/distance);
		int screenY = (int)(centerY + (y*scale)/distance);
		
		if( !g.hitClip(screenX, screenY, screenNodeSize, screenNodeSize) ) return;
		
		Rectangle oldClip = g.getClipBounds(new Rectangle());
		g.clipRect( screenX, screenY, screenNodeSize, screenNodeSize );
		
		if( n.background != null ) {
			drawPortal( n.background, x-n.backgroundX0, y-n.backgroundY0, n.backgroundSize, distance+n.backgroundDistance, g, scale, centerX, centerY);
		}
		if( n.image != null ) {
			g.drawImage(n.image.optimized(screenNodeSize,screenNodeSize), screenX, screenY, null);
		}
		float halfSize = nodeSize/2;
		if( n.n0 != null ) drawPortal( n.n0, x+0       , y+0       , halfSize, distance, g, scale, centerX, centerY);
		if( n.n1 != null ) drawPortal( n.n1, x+halfSize, y+0       , halfSize, distance, g, scale, centerX, centerY);
		if( n.n2 != null ) drawPortal( n.n2, x+0       , y+halfSize, halfSize, distance, g, scale, centerX, centerY);
		if( n.n3 != null ) drawPortal( n.n3, x+halfSize, y+halfSize, halfSize, distance, g, scale, centerX, centerY);
		
		g.setClip(oldClip.x, oldClip.y, oldClip.width, oldClip.height);
	}
}



/*

 XXXXXXXX
 XXXXXXXX
 XXXXXXXX
 XXXXXXXX
 XX####XX
 XX####XX
 XXXXXXXX
 XX####XX ___ center
 XX####XX
 XXXXXXXX
 XX####XX
 XX####XX
 XXXXXXXX
 XXXXXXXX
 XXXXXXXX
 XXXXXXXX
*/