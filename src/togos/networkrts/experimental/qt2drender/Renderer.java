package togos.networkrts.experimental.qt2drender;

import java.awt.image.BufferedImage;

/**
 * The goal is for this to be a general-purpose renderer
 * for 2D scenes with parallax backgrounds exposed through
 * portals
 */
public class Renderer
{
	public static class RenderNode {
		public static final Sprite[] EMPTY_SPRITE_LIST = new Sprite[0];
		public static final RenderNode EMPTY = new RenderNode(
			null, 0, 0, 0, 0,
			EMPTY_SPRITE_LIST, ImageHandle.EMPTY_ARRAY,
			null, null, null, null
		);
		
		final RenderNode background;
		/**
		 * Position of background node's top-left corner relative
		 * to this node's top-left corner
		 */
		final int backgroundCenterX, backgroundCenterY, backgroundSize;
		final int backgroundDistance;
		
		final Sprite[] sprites;
		
		final ImageHandle[] tileImages;
		final RenderNode n0, n1, n2, n3;
		
		/**
		 * z should increase monotonically
		 */
		static boolean spritesSortedProperly( Sprite[] sprites ) {
			float prevDist = Float.NEGATIVE_INFINITY;
			for( Sprite s : sprites ) {
				if( s.z < prevDist ) return false;
				prevDist = s.z;
			}
			return true;
		}
		
		public RenderNode( RenderNode background, int bgX, int bgY, int bgSize, int bgDistance, Sprite[] sprites, ImageHandle[] tileImages, RenderNode n0, RenderNode n1, RenderNode n2, RenderNode n3 ) {
			assert spritesSortedProperly(sprites);
			assert tileImages != null;
			
			this.background = background;
			this.backgroundCenterX = bgX;
			this.backgroundCenterY = bgY;
			this.backgroundSize = bgSize;
			this.backgroundDistance = bgDistance;
			this.sprites = sprites;
			this.tileImages = tileImages;
			this.n0 = n0;
			this.n1 = n1;
			this.n2 = n2;
			this.n3 = n3;
		}
		
		public RenderNode withSprite( Sprite...additionalSprites ) {
			if( additionalSprites.length == 0 ) return this;
			Sprite[] newSprites = new Sprite[sprites.length+additionalSprites.length];
			int i=0;
			for( Sprite s : sprites ) newSprites[i++] = s;
			for( Sprite s : additionalSprites ) newSprites[i++] = s;
			return new RenderNode(
				background, backgroundCenterX, backgroundCenterY, backgroundSize, backgroundDistance,
				newSprites, tileImages, n0, n1, n2, n3
			);
		}
	}
	
	public BufferedImage getSpriteImage( Sprite s, float optimizedForScale ) {
		// Don't bother optimizing unless in the parent node's plane
		return s.z == 0 ? s.image.optimized((int)(s.w*optimizedForScale),(int)(s.h*optimizedForScale)) : s.image.image;
	}
	
	public static void drawPortal(
		RenderNode n, float nodeSize, float wcx, float wcy, float distance,
		Display disp, float scx, float scy, float scale
	) {
		// clip to actual region on screen being drawn at
		
		float dscale = scale/distance; // Scale, taking distance into account
		
		float screenNodeSize = nodeSize*dscale;
		float screenX = scx - dscale*nodeSize/2;
		float screenY = scy - dscale*nodeSize/2;
		
		if( !disp.hitClip(screenX, screenY, screenNodeSize, screenNodeSize) ) return;
		
		disp.saveClip();
		disp.clip( screenX, screenY, screenNodeSize, screenNodeSize );
		
		if( n.background != null ) {
			drawPortal(
				n.background, n.backgroundSize, wcx+n.backgroundCenterX, wcy+n.backgroundCenterY, distance+n.backgroundDistance,
				disp, scale, scx, scy
			);
		}
		for( ImageHandle ih : n.tileImages ) {
			disp.draw(ih, screenX, screenY, screenNodeSize, screenNodeSize);
		}
		float halfSize = nodeSize/2;
		if( n.n0 != null ) drawPortal( n.n0, halfSize, wcx-halfSize, wcy-halfSize, distance, disp, scale, scx, scy);
		if( n.n1 != null ) drawPortal( n.n1, halfSize, wcx+halfSize, wcy-halfSize, distance, disp, scale, scx, scy);
		if( n.n2 != null ) drawPortal( n.n2, halfSize, wcx-halfSize, wcy+halfSize, distance, disp, scale, scx, scy);
		if( n.n3 != null ) drawPortal( n.n3, halfSize, wcx+halfSize, wcy+halfSize, distance, disp, scale, scx, scy);
		for( int si=0; si<n.sprites.length; ++si ) {
			Sprite s = n.sprites[si];
			float sdscale = scale/(distance-s.z);
			disp.draw(s.image, scy + (wcx+s.x)*sdscale, scy + (wcy+s.y)*sdscale, s.w*sdscale, s.h*sdscale);
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