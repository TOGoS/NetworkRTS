package togos.networkrts.experimental.qt2drender.demo;

import java.util.concurrent.Callable;

import togos.networkrts.experimental.qt2drender.ImageHandle;
import togos.networkrts.experimental.qt2drender.Sprite;
import togos.networkrts.experimental.qt2drender.Renderer.RenderNode;
import togos.networkrts.util.ResourceHandle;

public class NetRenderDemo
{
	static class BackgroundLink {
		/**
		 * Poisition of the background world
		 * relative to the vizstate's top-left corner
		 */
		public final float x, y, distance;
		public final ResourceHandle<RenderNode> background;
		public final float size;
		
		public BackgroundLink( float x, float y, float dist, ResourceHandle<RenderNode> background, float size ) {
			this.x = x; this.y = y; this.distance = dist;
			this.background = background; this.size = size;
		}
	}
	
	static class VizState {
		public final float offsetX, offsetY;
		public final int size; // edge length in tiles
		public final BackgroundLink[] backgroundPalette;
		public final byte[] cellBackgrounds;
		public final ResourceHandle<ImageHandle[]> tilePalette;
		public final byte[][] tileLayers;
		public final boolean[] cellVisibility;
		/**
		 * Should be sorted by z.
		 */
		public final Sprite[] sprites;
		
		protected static boolean isPowerOf2( int x ) {
			if( x < 0 ) return false;
			if( x == 0 ) return true;
			while( (x & 1) == 0 ) {
				x >>= 1;
			}
			return x == 1;
		}
		
		protected boolean assertProperlyFormed() {
			assert isPowerOf2(size);
			assert backgroundPalette != null;
			assert cellBackgrounds != null;
			assert cellBackgrounds.length == size;
			for( byte _bg : cellBackgrounds ) {
				int bg = _bg&0xFF;
				assert bg < backgroundPalette.length;
				assert backgroundPalette[bg] != null;
			}
			assert tilePalette != null;
			assert tileLayers != null;
			for( byte[] layer : tileLayers ) {
				assert layer != null;
				assert layer.length == size;
				// Can't check validity of tile indexes
				// since tile palette isn't available yet
			}
			assert cellVisibility.length == size;
			return true;
		}
		
		public VizState(
			float offsetX, float offsetY, int size,
			BackgroundLink[] backgroundPalette, byte[] cellBackgrounds,
			ResourceHandle<ImageHandle[]> tilePalette, byte[][] tileLayers, float[] tileLayerHeights,
			boolean[] cellVisibility, Sprite[] sprites
		) {
			this.offsetX = offsetX; this.offsetY = offsetY;
			this.size = size;
			this.backgroundPalette = backgroundPalette; this.cellBackgrounds = cellBackgrounds;
			this.tilePalette = tilePalette; this.tileLayers = tileLayers;
			this.cellVisibility = cellVisibility;
			this.sprites = sprites;
			
			assert assertProperlyFormed();
		}
	}
	
	static class RenderContext {
		Callable<ImageHandle[]> imagePaletteResolver;
		
		public ImageHandle[] getImagePalette( ResourceHandle<ImageHandle[]> handle ) {
			return handle.getValue(imagePaletteResolver);
		}
	}
	
	protected static int addSprites( Sprite[] src, int srcOffset, Sprite[] dest, int destOffset, float ceilZ ) {
		
	}
	
	protected static RenderNode toRenderNode( RenderContext ctx, VizState vs, Sprite[] scratch, int x, int y ) {
		int spriteIndex = 0;
		int layerIndex = 0;
		int spriteCount = 0;
		
		for( int i=0; i<vs.tileLayers.length; ++i )
		
	}
	
	protected static RenderNode toRenderNode( RenderContext ctx, VizState vs, Sprite[] scratch, int x, int y, int size ) {
		if( size == 1 ) return toRenderNode( ctx, vs, scratch, x, y );
			
	}
	
	public static RenderNode toRenderNode( RenderContext ctx, VizState vs ) {
		Sprite[] scratch = new Sprite[vs.sprites.length];
		return toRenderNode( ctx, vs, 0, 0, vs.size );
	}
}
