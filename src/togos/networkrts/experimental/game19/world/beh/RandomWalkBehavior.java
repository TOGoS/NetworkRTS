package togos.networkrts.experimental.game19.world.beh;

import java.util.List;
import java.util.Random;

import togos.networkrts.experimental.game19.world.Action;
import togos.networkrts.experimental.game19.world.ActionContext;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockBehavior;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.WorldNode;
import togos.networkrts.experimental.game19.world.gen.NodeUpdater;
import togos.networkrts.experimental.game19.world.gen.WorldUtil;

public class RandomWalkBehavior implements BlockBehavior
{
	interface CellSuitabilityChecker {
		public boolean cellIsSuitable( int x, int y, BlockStack bs );
	}
	
	static class FlagBasedCellSuitabilityChecker implements CellSuitabilityChecker {
		final int requiredFlags, bannedFlags;
		public FlagBasedCellSuitabilityChecker( int requiredFlags, int bannedFlags ) {
			this.requiredFlags = requiredFlags;
			this.bannedFlags = bannedFlags;
		}
		
		@Override
		public boolean cellIsSuitable( int x, int y, BlockStack bs ) {
			int flags = 0;
			for( Block b : bs.blocks ) {
				flags |= b.flags;
			}
			return
				((flags & requiredFlags) == requiredFlags) &&
				((flags & bannedFlags) == 0);
		}
	}
	
	static class MoveBlockAction implements Action, NodeUpdater {
		final Block block0;
		final int x0, y0;
		final Block block1;
		final int x1, y1;
		final CellSuitabilityChecker destinationChecker;
		
		public MoveBlockAction( Block b0, int x0, int y0, Block b1, int x1, int y1, CellSuitabilityChecker destinationChecker ) {
			this.block0 = b0;
			this.x0 = x0; this.y0 = y0;
			this.block1 = b1;
			this.x1 = x1; this.y1 = y1;
			this.destinationChecker = destinationChecker;
		}
		
		@Override public void apply( ActionContext ctx ) {
			WorldNode rn = ctx.getRootNode();
			int rx = ctx.getRootX();
			int ry = ctx.getRootY();
			int rsp = ctx.getRootSizePower();
			
			ctx.setRootNode( WorldUtil.updateNodeContaining( rn, rx, ry, rsp, x0, y0, x1+1, y1+1, this) );
		}

		@Override public WorldNode update( WorldNode node, int nodeX, int nodeY, int nodeSizePower ) {
			BlockStack bs = WorldUtil.getBlockStackAt( node, nodeX, nodeY, nodeSizePower, x1, y1 );
			if( bs == null ) {
				System.err.println("No destination block stack >:/");
				return node;
			}
			System.err.println("Trying to walk to "+x1+","+y1);
			if( destinationChecker.cellIsSuitable(x1, y1, bs) ) {
				System.err.println("It's clear!  Mving to "+x1+","+y1);
			} else {
				System.err.println("Arr, blocked!");
				return node;
			}
			
			node = WorldUtil.updateBlockStackAt( node, nodeX, nodeY, nodeSizePower, x0, y0, null, block0 );
			node = WorldUtil.updateBlockStackAt( node, nodeX, nodeY, nodeSizePower, x1, y1, block1, null );
			
			return node;
		}
	}
	
	public final long blockId;
	public final long nextStepTime;
	
	public RandomWalkBehavior( long blockId, long nextStepTime ) {
		this.blockId = blockId;
		this.nextStepTime = nextStepTime;
	}
	
	@Override public long getMinId() { return blockId; }
	@Override public long getMaxId() { return blockId; }
	@Override public long getNextAutoUpdateTime() { return nextStepTime; }
	@Override public Block update( Block b, int x, int y, int sizePower, long time,	Message[] messages, List<Action> results ) {
		// TODO: replace with some reproducable pseudo-random
		Random r = new Random();
		
		int destX, destY;
		switch( r.nextInt(4) ) {
		case 0: destX = x+1; destY = y  ; break;
		case 1: destX = x  ; destY = y+1; break;
		case 2: destX = x-1; destY = y  ; break;
		case 3: destX = x  ; destY = y-1; break;
		default: throw new RuntimeException("Unpossible!");
		}
		
		Block newBlock = new Block( b.imageHandle, b.flags, new RandomWalkBehavior(blockId, time+1) );
		
		results.add( new MoveBlockAction(b, x, y, newBlock, destX, destY, new FlagBasedCellSuitabilityChecker(0, Block.FLAG_SOLID) ) );
		return b;
	}
}
