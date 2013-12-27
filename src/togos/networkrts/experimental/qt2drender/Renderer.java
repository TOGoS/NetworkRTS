package togos.networkrts.experimental.qt2drender;

import java.awt.Graphics;
import java.awt.Rectangle;
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
		final int backgroundX0, backgroundY0, backgroundSize;
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
			this.backgroundX0 = bgX;
			this.backgroundY0 = bgY;
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
				background, backgroundX0, backgroundY0, backgroundSize, backgroundDistance,
				newSprites, tileImages, n0, n1, n2, n3
			);
		}
	}
	
	public BufferedImage getSpriteImage( Sprite s, float optimizedForScale ) {
		// Don't bother optimizing unless in the parent node's plane
		return s.z == 0 ? s.image.optimized((int)(s.w*optimizedForScale),(int)(s.h*optimizedForScale)) : s.image.image;
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
		
		float dscale = scale/distance; // Scale, taking distance into account
		
		int screenNodeSize = (int)Math.ceil((double)nodeSize*dscale);
		int screenX = (int)(centerX + x*dscale);
		int screenY = (int)(centerY + y*dscale);
		
		if( !g.hitClip(screenX, screenY, screenNodeSize, screenNodeSize) ) return;
		
		Rectangle oldClip = g.getClipBounds(new Rectangle());
		g.clipRect( screenX, screenY, screenNodeSize, screenNodeSize );
		
		if( n.background != null ) {
			drawPortal( n.background, x-n.backgroundX0, y-n.backgroundY0, n.backgroundSize, distance+n.backgroundDistance, g, scale, centerX, centerY);
		}
		for( ImageHandle ih : n.tileImages ) {
			g.drawImage(ih.optimized(screenNodeSize,screenNodeSize), screenX, screenY, null);
		}
		float halfSize = nodeSize/2;
		if( n.n0 != null ) drawPortal( n.n0, x+0       , y+0       , halfSize, distance, g, scale, centerX, centerY);
		if( n.n1 != null ) drawPortal( n.n1, x+halfSize, y+0       , halfSize, distance, g, scale, centerX, centerY);
		if( n.n2 != null ) drawPortal( n.n2, x+0       , y+halfSize, halfSize, distance, g, scale, centerX, centerY);
		if( n.n3 != null ) drawPortal( n.n3, x+halfSize, y+halfSize, halfSize, distance, g, scale, centerX, centerY);
		for( int si=0; si<n.sprites.length; ++si ) {
			Sprite s = n.sprites[si];
			float sdscale = scale/(distance-s.z);
			int sScreenX = (int)(centerX + (x+s.x)*sdscale);
			int sScreenY = (int)(centerY + (y+s.y)*sdscale);
			BufferedImage sImg = getSpriteImage(s, sdscale);
			g.drawImage(sImg,
				sScreenX, sScreenY,
				(int)(sScreenX+s.w*sdscale), (int)(sScreenY+s.w*sdscale),
				0, 0, sImg.getWidth(), sImg.getHeight(),
				null);
		}
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