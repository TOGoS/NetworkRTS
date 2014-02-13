package togos.networkrts.experimental.game19.world.beh;

import togos.networkrts.experimental.game19.world.Action;
import togos.networkrts.experimental.game19.world.ActionContext;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.NodeUpdater;
import togos.networkrts.experimental.game19.world.WorldNode;
import togos.networkrts.experimental.game19.world.WorldUtil;

/**
 * Removes and re-adds a specific snapshot of a block
 * if destinationChecker returns true at the destination.
 * 
 * Shouldn't use this for character movement, as the
 * player block may be updated before this action is applied,
 * in which case the wrong version will be removed/added.
 */
class MoveBlockAction implements Action, NodeUpdater
{
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
		
		int x0 = Math.min(this.x0, this.x1);
		int x1 = Math.max(this.x0, this.x1)+1;
		int y0 = Math.min(this.y0, this.y1);
		int y1 = Math.max(this.y0, this.y1)+1;
		
		ctx.setRootNode( WorldUtil.updateNodeContaining( rn, rx, ry, rsp, x0, y0, x1, y1, this) );
	}

	@Override public WorldNode update( WorldNode node, int nodeX, int nodeY, int nodeSizePower ) {
		BlockStack bs = WorldUtil.getBlockStackAt( node, nodeX, nodeY, nodeSizePower, x1, y1 );
		if( bs == null ) {
			System.err.println("No destination block stack >:/");
			return node;
		}
		if( !destinationChecker.cellIsSuitable(x1, y1, bs) ) {
			return node;
		}
		
		node = WorldUtil.updateBlockStackAt( node, nodeX, nodeY, nodeSizePower, x0, y0, null, block0 );
		node = WorldUtil.updateBlockStackAt( node, nodeX, nodeY, nodeSizePower, x1, y1, block1, null );
		
		return node;
	}
}