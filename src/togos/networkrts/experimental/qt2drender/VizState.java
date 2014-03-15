package togos.networkrts.experimental.qt2drender;

import java.io.Serializable;

import togos.networkrts.util.SoftResourceHandle;

public class VizState implements Serializable
{
	private static final long serialVersionUID = 1L;

	public static class BackgroundLink implements Serializable {
		private static final long serialVersionUID = 1L;
		
		public final SoftResourceHandle<QTRenderNode> background;
		public final float size;
		/**
		 * Position of the center of the background node
		 * relative to the vizstate's origin in world units
		 */
		public final float x, y, distance;
		
		public BackgroundLink( SoftResourceHandle<QTRenderNode> background, float size, float x, float y, float dist ) {
			this.background = background; this.size = size;
			this.x = x; this.y = y; this.distance = dist;
		}
	}
	
	public final int size; // edge length in tiles
	/**
	 * Position within vizstate data that should be considered 0, 0
	 */
	public final float originX, originY;
	/**
	 * Position (relative to origin) that should generally be centered on the screen.
	 */
	public final float focusX, focusY;
	public final VizState.BackgroundLink[] backgroundPalette;
	public final byte[] cellBackgrounds;
	public final SoftResourceHandle<ImageHandle[]> tilePalette;
	public final byte[][] tileLayers;
	public final boolean[] cornerVisibility;
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
		//assert isPowerOf2(size);
		assert backgroundPalette != null;
		assert cellBackgrounds != null;
		assert cellBackgrounds.length == size*size;
		for( byte _bg : cellBackgrounds ) {
			int bg = _bg&0xFF;
			assert bg < backgroundPalette.length;
			// assert backgroundPalette[bg] != null;
		}
		assert tilePalette != null;
		assert tileLayers != null;
		for( byte[] layer : tileLayers ) {
			assert layer != null;
			assert layer.length == size*size;
			// Can't check validity of tile indexes
			// since tile palette isn't available yet
		}
		assert cornerVisibility.length == (size+1)*(size+1);
		return true;
	}
	
	public VizState(
		int size, float originX, float originY, float focusX, float focusY,
		VizState.BackgroundLink[] backgroundPalette, byte[] cellBackgrounds,
		SoftResourceHandle<ImageHandle[]> tilePalette, byte[][] tileLayers,
		boolean[] cornerVisibility, Sprite[] sprites
	) {
		this.size = size;
		this.originX = originX; this.originY = originY;
		this.focusX = focusX; this.focusY = focusY;
		this.backgroundPalette = backgroundPalette; this.cellBackgrounds = cellBackgrounds;
		this.tilePalette = tilePalette; this.tileLayers = tileLayers;
		this.cornerVisibility = cornerVisibility;
		this.sprites = sprites;
		
		assert assertProperlyFormed();
	}
}