package togos.networkrts.experimental.game19.world.sim;

import java.util.ArrayList;
import java.util.List;

import togos.networkrts.experimental.game19.world.ActionContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockDynamics;
import togos.networkrts.experimental.game19.world.BlockInstance;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.WorldNode;
import togos.networkrts.experimental.game19.world.WorldUtil;
import togos.networkrts.util.BitAddressUtil;
import togos.networkrts.util.SimpleBitAddressRange;

public class PhysicsUpdater
{
	public static final SimpleBitAddressRange dynamicBitAddresses = BitAddresses.requiringFlags(BitAddresses.BLOCK_PHYS);
	
	static class BlockPointer {
		public final int blockId;
		public final int x, y;
		public BlockPointer( int blockId, int x, int y ) {
			this.blockId = blockId; this.x = x; this.y = y;
		}
	}
	
	protected static void findDynamicBlocks(WorldNode n, int x, int y, int sp, long time, List<BlockPointer> dest) {
		if( n.getNextAutoUpdateTime() > time ) return;
		if( !BitAddressUtil.rangesIntersect(n, dynamicBitAddresses) ) return; 
		
		switch( n.getNodeType() ) {
		case BLOCKSTACK:
			for( Block b : n.getBlocks() ) {
				if( BitAddressUtil.rangeContains(dynamicBitAddresses, b.bitAddress) && b.getNextAutoUpdateTime() <= time ) {
					int blockId = BitAddresses.extractId(b.bitAddress); 
					assert blockId != 0;
					dest.add( new BlockPointer(blockId, x, y) );
				}
			}
			break;
		case QUADTREE:
			int subSizePower = sp-1;
			int subSize = 1<<subSizePower;
			WorldNode[] subNodes = n.getSubNodes();
			findDynamicBlocks(subNodes[0], x        , y        , subSizePower, time, dest);
			findDynamicBlocks(subNodes[1], x+subSize, y        , subSizePower, time, dest);
			findDynamicBlocks(subNodes[2], x        , y+subSize, subSizePower, time, dest);
			findDynamicBlocks(subNodes[3], x+subSize, y+subSize, subSizePower, time, dest);
			break;
		default:
			throw new RuntimeException("Don't know how to find dynamic blocks within "+n.getNodeType()+" node");
		}
	}
	
	protected static ArrayList<BlockPointer> findDynamicBlocks(ActionContext ctx, long time) {
		ArrayList<BlockPointer> foundBlocks = new ArrayList<BlockPointer>();
		findDynamicBlocks(ctx.getNode(), ctx.getNodeX(), ctx.getNodeY(), ctx.getNodeSizePower(), time, foundBlocks);
		return foundBlocks;
	}
	
	protected static void updateBlock(ActionContext ctx, BlockPointer ptr, long time) {
		BlockInstance inst = WorldUtil.findBlock(ctx, ptr.blockId);
		if( inst == null ) return; // Maybe it was destroyed!
		
		BlockDynamics newDynamics = inst.block.dynamics.update(time, 1, true, 1);
		int dx = (int)Math.round(newDynamics.posX), dy = (int)Math.round(newDynamics.posY);
		if( dx != 0 || dy != 0 ) {
			int destX = ptr.x+dx, destY = ptr.y+dy;
			// Try to move it!
			BlockStack destStack = WorldUtil.getBlockStackAt(ctx.getNode(), ctx.getNodeX(), ctx.getNodeY(), ctx.getNodeSizePower(), destX, destY);
			boolean blocked = false;
			for( Block b : destStack.getBlocks() ) {
				if( (b.bitAddress & BitAddresses.BLOCK_SOLID) != 0 ) blocked = true;
			}
			newDynamics = newDynamics.repositioned();
			if( !blocked ) {
				ctx.setNode(WorldUtil.updateBlockStackAt(ctx, inst.x, inst.y, null, inst.block));
				ctx.setNode(WorldUtil.updateBlockStackAt(ctx, destX, destY, inst.block.withDynamics(newDynamics), null));
			}
		} else {
			ctx.setNode(WorldUtil.updateBlockStackAt(ctx, inst.x, inst.y, inst.block, inst.block.withDynamics(newDynamics)));
		}
	}
	
	public static void apply(ActionContext ctx, long time) {
		ArrayList<BlockPointer> dynBlocks = findDynamicBlocks(ctx, time);
		System.err.println("Will try to update "+dynBlocks.size());
		for( BlockPointer ptr : dynBlocks ) {
			updateBlock(ctx, ptr, time);
		}
	}
}
