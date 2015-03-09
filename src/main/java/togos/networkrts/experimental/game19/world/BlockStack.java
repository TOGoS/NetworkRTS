package togos.networkrts.experimental.game19.world;

import togos.networkrts.util.BitAddressRange;

public interface BlockStack extends BitAddressRange
{
	public Block[] getBlocks();
	public long getNextAutoUpdateTime();
}
