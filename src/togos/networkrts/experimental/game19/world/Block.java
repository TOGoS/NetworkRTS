package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.world.beh.NoBehavior;

public class Block
{
	public final BlockStack stack = BlockStack.create( this );
	
	public static final int FLAG_SOLID = 0x001;
	
	public final ImageHandle imageHandle;
	public final int flags;
	public final BlockBehavior behavior;
	
	public Block( ImageHandle imageHandle, int flags, BlockBehavior behavior ) {
		this.imageHandle = imageHandle;
		this.flags = flags;
		this.behavior = behavior;
	}
	
	public Block( ImageHandle imageHandle ) {
		this( imageHandle, 0, NoBehavior.instance );
	}
	
	public Block withBehavior( BlockBehavior beh ) {
		return new Block( imageHandle, flags, beh );
	}
}
