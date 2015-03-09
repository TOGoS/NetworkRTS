package togos.networkrts.experimental.game19.world.beh;

import togos.networkrts.experimental.game19.world.BlockStack;

interface CellSuitabilityChecker {
	public boolean cellIsSuitable( int x, int y, BlockStack bs );
}