package togos.networkrts.experimental.game19.scene;

import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlockStack;

public class VisibilityChecker
{
	public static boolean isSeeThrough( BlockStack bs ) {
		return bs != null && (bs.getMaxBitAddress() & BitAddresses.BLOCK_OPAQUE) == 0;
	}
	
	public static void _calculateVisibility( BlockStack[] blockStacks, int width, int height, int offset, int x, int y, byte[] visibility, int maxDist ) {
		assert maxDist <= 0xFF;
		if( maxDist > 0xFF ) maxDist = 0xFF;
		if( maxDist <= 0 ) return;
		
		int vIdx = x+width*y;
		if( (visibility[vIdx]&0xFF) >= maxDist ) return; // Already visited by a path closer to the viewer
		
		visibility[vIdx] = (byte)maxDist;
		
		--maxDist;
		
		if( !isSeeThrough(blockStacks[offset+vIdx]) ) return;
		
		if( x > 0        ) {
			if( y > 0        ) calculateVisibility(blockStacks, width, height, offset, x-1, y-1, visibility, maxDist );
			calculateVisibility(blockStacks, width, height, offset, x-1, y  , visibility, maxDist );
			if( y < height-1 ) calculateVisibility(blockStacks, width, height, offset, x-1, y+1, visibility, maxDist );
		}
		if( x < width-1  ) {
			if( y > 0        ) calculateVisibility(blockStacks, width, height, offset, x+1, y-1, visibility, maxDist );
			calculateVisibility(blockStacks, width, height, offset, x+1, y  , visibility, maxDist );
			if( y < height-1 ) calculateVisibility(blockStacks, width, height, offset, x+1, y+1, visibility, maxDist );
		}
		if( y > 0        ) calculateVisibility(blockStacks, width, height, offset, x  , y-1, visibility, maxDist );
		if( y < height-1 ) calculateVisibility(blockStacks, width, height, offset, x  , y+1, visibility, maxDist );
	}
	
	public static void calculateVisibility( BlockStack[] blockStacks, int width, int height, int offset, int x, int y, byte[] visibility, int maxDist ) {
		if( x < 0 || x >= width || y < 0 || y >= height ) return;
		_calculateVisibility( blockStacks, width, height, offset, x, y, visibility, maxDist );
	}
	
	public static void applyVisibilityToAllLayers( byte[] visibility, int width, int height, BlockStack[] blockStacks, int depth ) {
		final int area = width*height;
		for( int i=0, y=0; y<height; ++y ) for( int x=0; x<width; ++x, ++i ) {
			if( visibility[i] == 0 ) for( int z=0; z<depth; ++z ) {
				blockStacks[z*area + i] = null;
			}
		}
	}
	
	public static void calculateAndApplyVisibility( TileLayerData ld, int originX, int originY, int originZ, int maxDist ) {
		byte[] visibility = new byte[ld.width*ld.height];
		calculateVisibility( ld.blockStacks, ld.width, ld.height, originZ*ld.width*ld.height, originX, originY, visibility, maxDist );
		applyVisibilityToAllLayers(visibility, ld.width, ld.height, ld.blockStacks, ld.depth);
	}
}
