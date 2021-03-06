package togos.networkrts.experimental.game19.scene;

import java.util.Arrays;

import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;

public class TileLayerData {
	/** Width and height in cell units */
	public final int width, height, depth;
	public final BlockStack[] blockStacks;
	
	public TileLayerData( int w, int h, int d, BlockStack[] blockStacks ) {
		this.width = w; this.height = h; this.depth = d;
		this.blockStacks = blockStacks;
	}
	
	public TileLayerData( int w, int h, int d ) {
		this( w, h, d, new BlockStack[w*h*d] );
	}
	
	/// Shadey stuff
	
	public static final byte SHADE_NONE = 0;
	public static final byte SHADE_TL = 8, SHADE_TR = 4, SHADE_BL = 2, SHADE_BR = 1;
	public static final byte SHADE_ALL = 15;
	
	/**
	 * Indicates corner visibility for each x,y cell.
	 * Bit SHADE_[X] being set indicates that the [X] corner is visible.
	 */
	protected int[] shades;
	/** Layer for which shades were calculated. */
	protected int shadeLayer;
	
	protected final boolean blockStackIsOpaque( BlockStack bs ) {
		if( bs == null ) return true;
		Block[] blocks = bs.getBlocks();
		for( int i=blocks.length-1; i>=0; --i ) {
			if( (blocks[i].flags & Block.FLAG_OPAQUE) != 0 ) return true;
		}
		return false;
	}
	
	protected boolean[] getVertexVisibility(int layer) {
		final int vvWidth = width+1;
		final boolean[] vv = new boolean[vvWidth*(height+1)];
		Arrays.fill(vv, false);
		
		// Lighten corners of all non-opaque cells
		for( int y=0, i=width*height*layer; y<height; ++y ) for( int x=0, j=y*vvWidth; x<width; ++x, ++i, ++j ) {
			if( VisibilityChecker.isSeeThrough(blockStacks[i]) ) {
				vv[j        ] = true; vv[j        +1] = true;
				vv[j+vvWidth] = true; vv[j+vvWidth+1] = true;
			}
		}
		
		return vv;
	}
	
	protected static final byte getShade( boolean tl, boolean tr, boolean bl, boolean br ) {
		return (byte)(
			(tl ? SHADE_TL : 0) |
			(tr ? SHADE_TR : 0) |
			(bl ? SHADE_BL : 0) |
			(br ? SHADE_BR : 0)
		);
	}
	
	public synchronized int[] getShades( int layer ) {
		if( shades == null || shadeLayer != layer ) {
			final boolean[] vv = getVertexVisibility(layer);
			final int vvWidth = width+1; 
	
			shades = new int[width*height]; 
			for( int i=0, y=0; y<height; ++y ) for( int x=0, j=y*vvWidth; x<width; ++x, ++i, ++j ) {
				shades[i] = getShade(
					vv[j        ], vv[j        +1],
					vv[j+vvWidth], vv[j+vvWidth+1]
				);
			}
			this.shadeLayer = layer;
		}
		return shades;
	}
}
