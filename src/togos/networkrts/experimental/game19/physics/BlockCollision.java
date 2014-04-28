package togos.networkrts.experimental.game19.physics;

import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class BlockCollision
{
	public final Block block;
	public final double correctionX, correctionY;
	
	public BlockCollision( Block block, double correctionX, double correctionY ) {
		this.block = block;
		this.correctionX = correctionX;
		this.correctionY = correctionY;
	}
	
	protected static double jaque( double leftOverlap, double rightOverlap ) {
		if( leftOverlap < 0 && rightOverlap < 0 ) return 0;
		return leftOverlap < rightOverlap ? -leftOverlap : rightOverlap;
	}
	
	public static BlockCollision forOverlap( Block block, AABB a, int blockX, int blockY, int blockSize ) {
		return new BlockCollision( block,
			jaque( a.maxX - blockX, (blockX+blockSize) - a.minX ),
			jaque( a.maxY - blockY, (blockY+blockSize) - a.minY )
		);
	}
	
	protected static BlockCollision merge( BlockCollision a, BlockCollision b ) {
		if( a == null && b == null ) return null;
		if( b == null ) return a;
		if( a == null ) return b;
		BlockCollision xwise = Math.abs(a.correctionX) > Math.abs(b.correctionX) ? a : b;
		BlockCollision ywise = Math.abs(a.correctionY) > Math.abs(b.correctionY) ? a : b;
		BlockCollision blockwise = Math.abs(xwise.correctionX) > Math.abs(ywise.correctionY) ? xwise : ywise;
		// If corrections conflict, they cancel out
		return new BlockCollision(
			blockwise.block,
			a.correctionX > 0 && b.correctionX < 0 ? 0 : xwise.correctionX,
			a.correctionY > 0 && b.correctionY < 0 ? 0 : ywise.correctionY
		);
	}
	
	public static BlockCollision findCollisionWithRst(AABB a, RSTNode rst, int rstX, int rstY, int rstSizePower, long addyBits, long flagBits) {
		if( a.maxX <= rstX || a.maxY <= rstY ) return null;
		int rstSize = 1<<rstSizePower;
		if( a.minX >= rstX+rstSize || a.minY >= rstY+rstSize ) return null;
		if( (rst.getMaxBitAddress() & addyBits) == 0 ) return null; 
		
		switch( rst.getNodeType() ) {
		case QUADTREE:
			RSTNode[] subNodes = rst.getSubNodes();
			int subSizePower = rstSizePower-1;
			int subSize = 1<<subSizePower;
			return merge(
				merge(
					findCollisionWithRst(a, subNodes[0], rstX        , rstY        , subSizePower, addyBits, flagBits),
					findCollisionWithRst(a, subNodes[1], rstX+subSize, rstY        , subSizePower, addyBits, flagBits)
				),
				merge(
					findCollisionWithRst(a, subNodes[2], rstX        , rstY+subSize, subSizePower, addyBits, flagBits),
					findCollisionWithRst(a, subNodes[3], rstX+subSize, rstY+subSize, subSizePower, addyBits, flagBits)
				)
			);
		case BLOCKSTACK:
			for( Block block : rst.getBlocks() ) {
				BlockCollision bst = forOverlap(block, a, rstX, rstY, rstSize);
				if( bst != null ) return bst;
			}
			return null;
		default:
			throw new UnsupportedOperationException("Unrecognized RST node type "+rst.getNodeType());
		}
	}

	public static BlockCollision findCollisionWithRst(AABB aabb, World w, long addyBits, long flagBits) {
		int rad = 1<<(w.rstSizePower-1);
		return findCollisionWithRst(aabb, w.rst, -rad, -rad, w.rstSizePower, addyBits, flagBits);
	}
	
	/*
	public static BlockCollision findCollisionWithRst(NonTile nt, World w, long addyBits, long flagBits) {
		return findCollisionWithRst(nt.getAbsolutePhysicalAabb(), w, addyBits, flagBits);
	}
	*/
}
