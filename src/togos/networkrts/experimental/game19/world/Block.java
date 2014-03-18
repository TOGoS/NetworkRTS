package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.world.beh.NoBehavior;
import togos.networkrts.util.BitAddressRange;
import togos.networkrts.util.BitAddressUtil;

public class Block implements BitAddressRange, HasAutoUpdateTime
{
	public final BlockStackRSTNode stack;
	
	public final long bitAddress;
	public final Icon icon;
	public final BlockBehavior behavior;
	
	public Block( long bitAddress, Icon icon, BlockBehavior behavior ) {
		this.bitAddress = BitAddresses.forceType( BitAddresses.TYPE_BLOCK, bitAddress );
		this.icon = icon;
		this.behavior = behavior;
		this.stack = BlockStackRSTNode.create( this );
	}
	
	public Block( Icon icon ) {
		this( 0, icon, NoBehavior.instance );
	}
	
	public Block withBehavior( BlockBehavior beh ) {
		return new Block( bitAddress, icon, beh );
	}
	
	@Override public long getMinBitAddress() {
		return BitAddressUtil.minAddress( bitAddress, behavior.getMinBitAddress() );
	}
	
	@Override public long getMaxBitAddress() {
		return BitAddressUtil.maxAddress( bitAddress, behavior.getMaxBitAddress() );
	}
	
	@Override public long getNextAutoUpdateTime() {
		return behavior.getNextAutoUpdateTime();
	}
}
