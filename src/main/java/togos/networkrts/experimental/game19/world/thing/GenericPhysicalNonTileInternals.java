package togos.networkrts.experimental.game19.world.thing;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class GenericPhysicalNonTileInternals extends AbstractPhysicalNonTileInternals
{
	protected final Icon icon;
	protected final AABB relativePhysicalAabb;
	
	public GenericPhysicalNonTileInternals( Icon icon, AABB relativePhysicalAabb ) {
		this.icon = icon;
		this.relativePhysicalAabb = relativePhysicalAabb;
	}
	
	@Override public Icon getIcon() { return icon; }
	
	@Override public AABB getRelativePhysicalAabb() { return relativePhysicalAabb; }
	
	@Override public long getNonTileAddressFlags() {
		return
			BitAddresses.PHYSINTERACT |
			BitAddresses.RIGIDBODY;
	}
	
	@Override public long getNextAutoUpdateTime() {
		return Long.MAX_VALUE;
	}
}
