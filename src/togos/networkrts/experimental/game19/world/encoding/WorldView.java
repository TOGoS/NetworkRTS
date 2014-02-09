package togos.networkrts.experimental.game19.world.encoding;

import togos.networkrts.experimental.game19.scene.LayerData;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.WorldNode;

public class WorldView
{
	public static void nodeToBlockArray( WorldNode n, int nx, int ny, int nsize, BlockStack[] blockStacks, int bx, int by, int bw, int bh, int bo ) {
		if( nx >= bx + bw || nx + nsize <= bx || ny >= by + bh || ny + nsize <= by ) return;
		if( n.isLeaf() ) {
			int rx = nx - bx;
			int ry = ny - by;
			blockStacks[rx + ry*bw + bo] = n.getBlockStack();
		} else {
			int subSize = nsize>>1;
			WorldNode[] subNodes = n.getSubNodes();
			for( int sy=0, si=0; sy<2; ++sy) for( int sx=0; sy<2; ++sx, ++si ) {
				nodeToBlockArray( subNodes[si], nx+(sx*subSize), ny+(sy*subSize), subSize, blockStacks, bx, by, bw, bh, bo );
			}
		}
	}
	
	public static void nodeToLayerData( WorldNode n, int nx, int ny, int nz, int nsize, LayerData layerData, int lx, int ly, int lw, int lh ) {
		nodeToBlockArray( n, nx, ny, nsize, layerData.blockStacks, lx, ly, lw, lh, nz*lw*lh );
	}
}
