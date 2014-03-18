package togos.networkrts.experimental.qt2drender;

import togos.networkrts.experimental.qt2drender.demo.NetRenderDemo;
import togos.networkrts.util.ResourceNotFound;

/**
 * The goal is for this to be a general-purpose renderer
 * for 2D scenes with parallax backgrounds exposed through
 * portals
 */
public class Renderer
{
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
	) throws ResourceNotFound {
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

	/**
	 * @param vs
	 * @param wcx X position at which to draw VizState, in world units, relative to display center
	 * @param wcy Y position at which to draw VizState, in world units, relative to display center
	 * @param distance
	 * @param disp
	 * @param scx
	 * @param scy
	 * @param scale
	 * @param ctx
	 * @throws ResourceNotFound
	 */
	public static void draw(
		VizState vs, float wcx, float wcy, float distance,
		Display disp, float scx, float scy, float scale, NetRenderDemo.RenderContext ctx
	) throws ResourceNotFound {
		ImageHandle[] tileImages = ctx.getImagePalette(vs.tilePalette);
		QTRenderNode[] backgroundNodes = ctx.getRenderNodes(vs.backgroundPalette);
		
		float dscale = scale/distance;
		
		for( int ti=0, ty=0; ty<vs.size; ++ty ) for( int tx=0; tx<vs.size; ++tx, ++ti ) {
			VizState.BackgroundLink bgLink = vs.backgroundPalette[vs.cellBackgrounds[ti]&0xFF];
			if( bgLink == null ) continue;
			QTRenderNode bg = backgroundNodes[vs.cellBackgrounds[ti]&0xFF];
			float bgDistance = distance + bgLink.distance;
			disp.saveClip();
			disp.clip(
				scx + (dscale*(wcx+tx-vs.originX)), scy + (dscale*(wcy+ty-vs.originY)),
				dscale, dscale
			);
			drawPortal(
				bg, bgLink.size, wcx+bgLink.x, wcy+bgLink.y, bgDistance,
				disp, scx, scy, scale
			);
			disp.restoreClip();
		}
		
		final float cellSize = scale/distance;
		int spriteIdx = 0;
		for( int l=0; l<vs.tileLayers.length; ++l ) {
			for( int ty=0; ty<vs.size; ++ty ) for( int tx=0; tx<vs.size; ++tx ) {
				if( NetRenderDemo.cellIsCompletelyInvisible(vs,tx,ty) ) continue;
				disp.draw(
					tileImages[vs.tileLayers[l][ty*vs.size+tx]],
					scx + (cellSize*(tx+wcx-vs.originX)), scy + (cellSize*(ty+wcy-vs.originY)),
					cellSize, cellSize
				);
			}
			while( spriteIdx < vs.sprites.length && vs.sprites[spriteIdx].z < l+1 ) {
				Sprite s = vs.sprites[spriteIdx]; 
				disp.draw(
					s.image,
					scx + (cellSize*(wcx+s.x-vs.originX)), scy + (cellSize*(wcy+s.y-vs.originY)),
					s.w, s.h
				);
				++spriteIdx;
			}
		}
		
		for( int ty=0; ty<vs.size; ++ty ) for( int tx=0; tx<vs.size; ++tx ) {
			int sp1 = vs.size+1;
			int idx0 = sp1*ty+tx;
			int idx1 = idx0+1;
			int idx2 = idx0+sp1;
			int idx3 = idx1+sp1;
			disp.draw(
				ctx.getFogImage(
					vs.cornerVisibility[idx0], vs.cornerVisibility[idx1],
					vs.cornerVisibility[idx2], vs.cornerVisibility[idx3]
				),
				scx + (cellSize*(wcx+tx-vs.originX)), scy + (cellSize*(wcy+ty-vs.originY)),
				cellSize, cellSize
			);
		}
	}
}
