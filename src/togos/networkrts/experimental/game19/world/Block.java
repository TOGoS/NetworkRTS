package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.world.beh.NoBehavior;
import togos.networkrts.util.BitAddressRange;
import togos.networkrts.util.BitAddressUtil;

public class Block implements BitAddressRange, HasAutoUpdateTime
{
	public final BlockStackNode stack;
	
	public final long bitAddress;
	public final ImageHandle imageHandle;
	public final BlockBehavior behavior;
	
	public Block( long bitAddress, ImageHandle imageHandle, BlockBehavior behavior ) {
		this.bitAddress = BitAddresses.forceType( BitAddresses.TYPE_BLOCK, bitAddress );
		this.imageHandle = imageHandle;
		this.behavior = behavior;
		this.stack = BlockStackNode.create( this );
	}
	
	public Block( ImageHandle imageHandle ) {
		this( 0, imageHandle, NoBehavior.instance );
	}
	
	public Block withBehavior( BlockBehavior beh ) {
		return new Block( bitAddress, imageHandle, beh );
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
