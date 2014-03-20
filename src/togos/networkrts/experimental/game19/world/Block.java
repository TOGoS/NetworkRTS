package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.world.beh.NoBehavior;
import togos.networkrts.util.BitAddressRange;
import togos.networkrts.util.BitAddressUtil;

public class Block implements BitAddressRange, HasAutoUpdateTime
{
	public static final long FLAG_SOLID  = 0x0000000000000001l; 
	public static final long FLAG_OPAQUE = 0x0000000000000002l;
	// TODO: Replace spikey with some more flexible mechanism
	public static final long FLAG_SPIKEY = 0x0000000000000004l;
	
	public final BlockStackRSTNode stack;
		
	public final long bitAddress;
	/** Additional flags that are not part of the bit address */
	public final long flags;
	public final Icon icon;
	public final BlockBehavior behavior;
	
	public Block( long bitAddress, long flags, Icon icon, BlockBehavior behavior ) {
		this.bitAddress = BitAddresses.forceType( BitAddresses.TYPE_BLOCK, bitAddress );
		this.flags = flags;
		this.icon = icon;
		this.behavior = behavior;
		this.stack = BlockStackRSTNode.create( this );
	}
	
	public Block( Icon icon ) {
		this( 0, 0, icon, NoBehavior.instance );
	}
	
	public Block withBehavior( BlockBehavior beh ) {
		return new Block( bitAddress, flags, icon, beh );
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
