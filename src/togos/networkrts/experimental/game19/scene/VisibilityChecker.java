package togos.networkrts.experimental.game19.scene;

import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;

public class VisibilityChecker
{
	protected static boolean isSeeThrough( BlockStack bs ) {
		if( bs == null ) return false;
		for( Block b : bs.blocks ) {
			if( (b.bitAddress & BitAddresses.BLOCK_OPAQUE) == BitAddresses.BLOCK_OPAQUE ) return false;
		}
		return true;
	}
	
	// TODO: Better algorithm.
	
	/*
	protected static void calculateVisibility( BlockStack[] blockStacks, int width, int height, int offset, int originX, int originY, int dx, int dy, boolean[] visibility ) {
		for( int x=originX, y=originY; x>=0 && x<width && y>=0 && y<height; x+=dx, y+=dy ) {
			int idx = x + y*width;
			BlockStack bs = blockStacks[offset + idx];
			visibility[idx] = true;
			if( !isSeeThrough(bs) ) return;
		}		
	}
	
	protected static void calculateVisibility( BlockStack[] blockStacks, int width, int height, int offset, int originX, int originY, int dx, int dy, int dx1, int dy1, int dx2, int dy2, boolean[] visibility ) {
		for( int x=originX, y=originY; x>=0 && x<width && y>=0 && y<height; x+=dx, y+=dy ) {
			int idx = x + y*width;
			BlockStack bs = blockStacks[offset + idx];
			visibility[idx] = true;
			if( !isSeeThrough(bs) ) return;
				
			calculateVisibility( blockStacks, width, height, offset, x+dx1, y+dy1, dx1, dy1, visibility );
			calculateVisibility( blockStacks, width, height, offset, x+dx2, y+dy2, dx2, dy2, visibility );
		}
	}
	
	public static void calculateVisibility( BlockStack[] blockStacks, int width, int height, int offset, int originX, int originY, boolean[] visibility ) {
		calculateVisibility( blockStacks, width, height, offset, originX, originY,  1, 0, 0, 1, 0,-1, visibility );
		calculateVisibility( blockStacks, width, height, offset, originX, originY, -1, 0, 0, 1, 0,-1, visibility );
		calculateVisibility( blockStacks, width, height, offset, originX, originY,  0, 1, 1, 0,-1, 0, visibility );
		calculateVisibility( blockStacks, width, height, offset, originX, originY,  0,-1, 1, 0,-1, 0, visibility );
	}
	*/
	
	public static void _calculateVisibility( BlockStack[] blockStacks, int width, int height, int offset, int x, int y, boolean[] visibility ) {
		int vIdx = x+width*y;
		if( visibility[vIdx] ) return;
		
		visibility[vIdx] = true;
		
		if( !isSeeThrough(blockStacks[offset+vIdx]) ) return;
		

		if( x > 0        ) {
			if( y > 0        ) calculateVisibility(blockStacks, width, height, offset, x-1, y-1, visibility );
			calculateVisibility(blockStacks, width, height, offset, x-1, y  , visibility );
			if( y < height-1 ) calculateVisibility(blockStacks, width, height, offset, x-1, y+1, visibility );
		}
		if( x < width-1  ) {
			if( y > 0        ) calculateVisibility(blockStacks, width, height, offset, x+1, y-1, visibility );
			calculateVisibility(blockStacks, width, height, offset, x+1, y  , visibility );
			if( y < height-1 ) calculateVisibility(blockStacks, width, height, offset, x+1, y+1, visibility );
		}
		if( y > 0        ) calculateVisibility(blockStacks, width, height, offset, x  , y-1, visibility );
		if( y < height-1 ) calculateVisibility(blockStacks, width, height, offset, x  , y+1, visibility );
	}
	
	public static void calculateVisibility( BlockStack[] blockStacks, int width, int height, int offset, int x, int y, boolean[] visibility ) {
		if( x < 0 || x >= width || y < 0 || y >= height ) return;
		_calculateVisibility( blockStacks, width, height, offset, x, y, visibility );
		/*
		int vIdx = x+width*y;
		if( visibility[vIdx] ) return;
		
		visibility[vIdx] = true;

		for( int dy=-1; dy<=1; ++dy ) for( int dx=-1; dx<=1; ++dx ) if( dx != 0 && dy != 0 ) {
			calculateVisibility(blockStacks, width, height, offset, x+dx, y+dy, visibility );
		}
		*/
	}
	
	public static void applyVisibilityToAllLayers( boolean[] visibility, int width, int height, BlockStack[] blockStacks, int depth ) {
		final int area = width*height;
		for( int i=0, y=0; y<height; ++y ) for( int x=0; x<width; ++x, ++i ) {
			if( !visibility[i] ) for( int z=0; z<depth; ++z ) {
				blockStacks[z*area + i] = null;
			}
		}
	}
	
	public static void calculateAndApplyVisibility( LayerData ld, int originX, int originY, int originZ ) {
		boolean[] visibility = new boolean[ld.width*ld.height];
		calculateVisibility( ld.blockStacks, ld.width, ld.height, originZ*ld.width*ld.height, originX, originY, visibility );
		applyVisibilityToAllLayers(visibility, ld.width, ld.height, ld.blockStacks, ld.depth);
	}
}
