package togos.networkrts.experimental.game19.world;

import java.util.List;

import togos.networkrts.util.BitAddressRange;

/**
 * The range returned by BitAddressRange does not need to include
 * the block's address.  Its purpose is to allow the behavior to
 * catch messages other than the ones directed to the block.
 */
public interface BlockBehavior extends BitAddressRange, HasAutoUpdateTime
{
	public Block update( Block b, int x, int y, int sizePower, long time, Message[] messages, List<Action> results );
}
