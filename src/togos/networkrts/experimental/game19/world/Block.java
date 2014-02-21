package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.world.beh.NoBehavior;
import togos.networkrts.util.BitAddressRange;
import togos.networkrts.util.BitAddressUtil;

public class Block implements BitAddressRange, HasAutoUpdateTime
{
	public final BlockStack stack = BlockStack.create( this );
	
	public final long bitAddress;
	public final ImageHandle imageHandle;
	public final BlockBehavior behavior;
	public final BlockDynamics dynamics;
	
	public Block( long bitAddress, ImageHandle imageHandle, BlockBehavior behavior, BlockDynamics dynamics ) {
		this.bitAddress = BitAddresses.forceType( BitAddresses.TYPE_BLOCK, bitAddress );
		this.imageHandle = imageHandle;
		this.behavior = behavior;
		this.dynamics = dynamics;
	}
	
	public Block( ImageHandle imageHandle ) {
		this( 0, imageHandle, NoBehavior.instance, BlockDynamics.NONE );
	}
	
	public Block withBehavior( BlockBehavior beh ) {
		return new Block( bitAddress, imageHandle, beh, dynamics );
	}
	
	@Override public long getMinBitAddress() {
		return BitAddressUtil.minAddress( bitAddress, behavior.getMinBitAddress() );
	}
	
	@Override public long getMaxBitAddress() {
		return BitAddressUtil.maxAddress( bitAddress, behavior.getMaxBitAddress() );
	}
	
	@Override public long getNextAutoUpdateTime() {
		return Math.min(dynamics.getNextAutoUpdateTime(), behavior.getNextAutoUpdateTime());
	}

	public Block withDynamics(BlockDynamics dynamics) {
		return new Block( bitAddress, imageHandle, behavior, dynamics );
	}
}
