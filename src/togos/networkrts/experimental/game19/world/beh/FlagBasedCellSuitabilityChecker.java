package togos.networkrts.experimental.game19.world.beh;

import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;

class FlagBasedCellSuitabilityChecker implements CellSuitabilityChecker {
	final long requiredFlags, bannedFlags;
	public FlagBasedCellSuitabilityChecker( long requiredFlags, long bannedFlags ) {
		this.requiredFlags = requiredFlags;
		this.bannedFlags = bannedFlags;
	}
	
	@Override
	public boolean cellIsSuitable( int x, int y, BlockStack bs ) {
		long flags = 0;
		for( Block b : bs.blocks ) {
			flags |= b.bitAddress;
		}
		return
			((flags & requiredFlags) == requiredFlags) &&
			((flags & bannedFlags) == 0);
	}
}