package togos.networkrts.experimental.qt2drender;

import java.awt.image.BufferedImage;

/**
 * The goal is for this to be a general-purpose renderer
 * for 2D scenes with parallax backgrounds exposed through
 * portals
 */
public class Renderer
{
	public BufferedImage getSpriteImage( Sprite s, float optimizedForScale ) {
		// Don't bother optimizing unless in the parent node's plane
		return s.z == 0 ? s.image.optimized((int)(s.w*optimizedForScale),(int)(s.h*optimizedForScale)) : s.image.image;
	}

	/**
	 * @param n node to be drawn
	 * @param nodeSize width and height (in world units) of node
	 * @param ncx node center X, in world units, relative to the center of the display
	 * @param ncy node center Y, in world units, relative to the center of the display
	 * @param distance distance from display to node (positive for nodes that are in front of the camera)
	 * @param disp display to be drawn on
	 * @param scx screen center X, in pixels, relative to the left side of the display 
	 * @param scy screen center Y, in pixels, relative to the top of the display
	 * @param scale how big to draw everything.  at scale = 1, a 1 world-unit node at distance 1 will be 1 pixel wide.
	 */
	public static void drawPortal(
		QTRenderNode n, float nodeSize, float ncx, float ncy, float distance,
		Display disp, float scx, float scy, float scale
	) {
		// clip to actual region on screen being drawn at
		
		float dscale = scale/distance; // Scale, taking distance into account
		
		float screenNodeSize = nodeSize*dscale;
		float screenX = scx + (ncx-nodeSize/2)*dscale;
		float screenY = scy + (ncy-nodeSize/2)*dscale;
		
		if( !disp.hitClip(screenX, screenY, screenNodeSize, screenNodeSize) ) return;
		
		disp.saveClip();
		disp.clip( screenX, screenY, screenNodeSize, screenNodeSize );
		
		if( n.background != null ) {
			drawPortal(
				n.background, n.backgroundSize, ncx+n.backgroundCenterX, ncy+n.backgroundCenterY, distance+n.backgroundDistance,
				disp, scx, scy, scale
			);
		}
		for( ImageHandle ih : n.tileImages ) {
			disp.draw(ih, screenX, screenY, screenNodeSize, screenNodeSize);
		}
		float halfSize = nodeSize/2f;
		float quarterSize = halfSize/2;
		if( n.n0 != null ) drawPortal( n.n0, halfSize, ncx-quarterSize, ncy-quarterSize, distance, disp, scx, scy, scale);
		if( n.n1 != null ) drawPortal( n.n1, halfSize, ncx+quarterSize, ncy-quarterSize, distance, disp, scx, scy, scale);
		if( n.n2 != null ) drawPortal( n.n2, halfSize, ncx-quarterSize, ncy+quarterSize, distance, disp, scx, scy, scale);
		if( n.n3 != null ) drawPortal( n.n3, halfSize, ncx+quarterSize, ncy+quarterSize, distance, disp, scx, scy, scale);
		for( int si=0; si<n.sprites.length; ++si ) {
			Sprite s = n.sprites[si];
			float sdscale = scale/(distance-s.z);
			disp.draw(s.image, scx + (ncx+s.x)*sdscale, scy + (ncy+s.y)*sdscale, s.w*sdscale, s.h*sdscale);
		}
		disp.restoreClip();
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