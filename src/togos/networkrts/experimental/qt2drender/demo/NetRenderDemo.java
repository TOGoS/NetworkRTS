package togos.networkrts.experimental.qt2drender.demo;

import java.io.InputStream;
import java.io.ObjectInputStream;

import togos.blob.InputStreamable;
import togos.networkrts.experimental.qt2drender.Display;
import togos.networkrts.experimental.qt2drender.ImageHandle;
import togos.networkrts.experimental.qt2drender.Renderer.RenderNode;
import togos.networkrts.experimental.qt2drender.Sprite;
import togos.networkrts.util.Getter;
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
		Getter<InputStreamable> blobResolver;
		
		protected <T> Getter<T> makeGetter( final Class<T> c ) {
			return new Getter<T>() {
				@Override public T get(String uri) throws Exception {
					InputStreamable blob = blobResolver.get(uri);
					InputStream is = blob.openInputStream();
					try {
						ObjectInputStream ois = new ObjectInputStream(is);
						return c.cast(ois.readObject());
					} finally {
						is.close();
					}
				}
			};
		}
		
		Getter<ImageHandle[]> imagePaletteResolver = makeGetter(ImageHandle[].class);
		Getter<RenderNode> renderNodeResolver = makeGetter(RenderNode.class);
		
		public ImageHandle[] getImagePalette( ResourceHandle<ImageHandle[]> handle ) {
			return handle.getValue(imagePaletteResolver);
		}
		
		public RenderNode getRenderNode( ResourceHandle<RenderNode> handle ) {
			return handle.getValue(renderNodeResolver);
		}
		
		public RenderNode[] getRenderNodes( BackgroundLink[] links ) {
			RenderNode[] nodes = new RenderNode[links.length];
			for( int i=0; i<links.length; ++i ) {
				nodes[i] = getRenderNode(links[i].background);
			}
			return nodes;
		}
	}
	
	public static void draw( VizState vs, RenderContext ctx, Display disp, float scale, float scx, float csy, float x, float y, float distance ) {
		ImageHandle[] tileImages = ctx.getImagePalette(vs.tilePalette);
		RenderNode[] backgroundNodes = ctx.getRenderNodes(vs.backgroundPalette);
		
		// TODO
		// Draw all backgrounds
		// Draw foreground layers
		// Draw gradient around visibilty edge
		
		for( int ty=0; ty<vs.size; ++ty ) for( int tx=0; tx<vs.size; ++tx ) {
			// uhm
		}
	}
}
