package togos.networkrts.experimental.game19.world.beh;

import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;

class FlagBasedCellSuitabilityChecker implements CellSuitabilityChecker {
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