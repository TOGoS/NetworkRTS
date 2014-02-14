package togos.networkrts.experimental.game19.scene;

import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;

public class VisibilityChecker
{
	protected static boolean isSeeThrough( BlockStack bs ) {
		if( bs == null ) return false;
		for( Block b : bs.blocks ) {
			if( (b.flags & Block.FLAG_OPAQUE) == Block.FLAG_OPAQUE ) return false;
		}
		return true;
	}
	
	// TODO: Better algorithm.
	
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
