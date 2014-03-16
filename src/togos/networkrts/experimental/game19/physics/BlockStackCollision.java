package togos.networkrts.experimental.game19.physics;

import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class BlockStackCollision
{
	public final BlockStack blockStack;
	public final double correctionX, correctionY;
	
	public BlockStackCollision( BlockStack bs, double correctionX, double correctionY ) {
		this.blockStack = bs;
		this.correctionX = correctionX;
		this.correctionY = correctionY;
	}
	
	protected static double jaque( double leftOverlap, double rightOverlap ) {
		if( leftOverlap < 0 && rightOverlap < 0 ) return 0;
		return leftOverlap < rightOverlap ? -leftOverlap : rightOverlap;
	}
	
	public static BlockStackCollision forOverlap( BlockStack bs, AABB a, int blockX, int blockY, int blockSize ) {
		//return new Collision( bs, 0, blockY-a.maxY);
		return new BlockStackCollision( bs,
			jaque( a.maxX - blockX, (blockX+blockSize) - a.minX ),
			jaque( a.maxY - blockY, (blockY+blockSize) - a.minY )
		);
	}

	public static BlockStackCollision findCollisionWithRst(AABB a, RSTNode rst, int rstX, int rstY, int rstSizePower, long tileFlags) {
		if( a.maxX <= rstX || a.maxY <= rstY ) return null;
		int rstSize = 1<<rstSizePower;
		if( a.minX >= rstX+rstSize || a.minY >= rstY+rstSize ) return null;
		if( (rst.getMaxBitAddress() & tileFlags) == 0 ) return null; 
		
		switch( rst.getNodeType() ) {
		case QUADTREE:
			RSTNode[] subNodes = rst.getSubNodes();
			int subSizePower = rstSizePower-1;
			int subSize = 1<<subSizePower;
			BlockStackCollision c;
			if( (c = findCollisionWithRst(a, subNodes[0], rstX        , rstY        , subSizePower, tileFlags)) != null ) return c;
			if( (c = findCollisionWithRst(a, subNodes[1], rstX+subSize, rstY        , subSizePower, tileFlags)) != null ) return c;
			if( (c = findCollisionWithRst(a, subNodes[2], rstX        , rstY+subSize, subSizePower, tileFlags)) != null ) return c;
			if( (c = findCollisionWithRst(a, subNodes[3], rstX+subSize, rstY+subSize, subSizePower, tileFlags)) != null ) return c;
			return null;
		case BLOCKSTACK:
			return forOverlap(rst, a, rstX, rstY, rstSize);
		default:
			throw new UnsupportedOperationException("Unrecognized RST node type "+rst.getNodeType());
		}
	}

	public static BlockStackCollision findCollisionWithRst(NonTile nt, World w, long tileFlag) {
		int rad = 1<<(w.rstSizePower-1);
		return findCollisionWithRst(nt.absolutePhysicalAabb, w.rst, -rad, -rad, w.rstSizePower, tileFlag);
	}
}
