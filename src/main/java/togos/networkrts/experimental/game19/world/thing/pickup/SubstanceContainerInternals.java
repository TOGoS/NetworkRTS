package togos.networkrts.experimental.game19.world.thing.pickup;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.thing.AbstractPhysicalNonTileInternals;
import togos.networkrts.experimental.game19.world.thing.Substance;
import togos.networkrts.experimental.game19.world.thing.SubstanceQuantity;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class SubstanceContainerInternals extends AbstractPhysicalNonTileInternals
{
	public final SubstanceContainerType type;
	public final SubstanceQuantity contents;
	public final long nextAutoUpdateTime;
	
	protected SubstanceContainerInternals( SubstanceContainerType type, SubstanceQuantity contents, long nextAut ) {
		this.type = type;
		this.contents = contents;
		this.nextAutoUpdateTime = nextAut;
	}
	
	public static SubstanceContainerInternals filled( SubstanceContainerType type, Substance filler ) {
		return new SubstanceContainerInternals(type, new SubstanceQuantity(filler, type.getCapacity(filler)), 0);
	}
	
	protected SubstanceContainerInternals withNextAutoUpdateTime( long aut ) {
		return aut == this.nextAutoUpdateTime ? this : new SubstanceContainerInternals(type, contents, aut);
	}
	
	@Override public BlargNonTile update(
		BlargNonTile nt0, long time, World world, MessageSet messages, NonTileUpdateContext updateContext
	) {
		final BlargNonTile nt = super.update(nt0, time, world, messages, updateContext);
		
		for( Message m : messages ) {
			if( nt.isSpecificallyAddressedBy(m) ) {
				switch( m.type ) {
				case REQUEST_PICKUP:
					updateContext.sendMessage(Message.create(m.sourceAddress, MessageType.INCOMING_ITEM, nt));
					return null;
				default: // Ignore everything else
				}
			}
		}
		
		return nt;
	}
	
	@Override public Icon getIcon() {
		int iconIdx = (int)Math.round(fullness() * (type.icons.length-1));
		iconIdx = iconIdx < 0 ? 0 : iconIdx >= type.icons.length ? type.icons.length-1 : iconIdx;
		return type.icons[iconIdx];
	}
	@Override public AABB getRelativePhysicalAabb() { return type.relativePhysicalAabb; }
	@Override public long getNonTileAddressFlags() {
		return
			BitAddresses.PHYSINTERACT |
			BitAddresses.PICKUP;
	}
	@Override public long getNextAutoUpdateTime() {
		return nextAutoUpdateTime;
	}
	
	public double getCapacity() {
		return type.getCapacity(contents.substance);
	}
	
	public SubstanceContainerInternals add(double delta) {
		return new SubstanceContainerInternals(type, new SubstanceQuantity(contents.substance, contents.quantity + delta), 0);
	}
	
	public double fullness() {
		return contents.quantity / getCapacity();
	}
	
	public boolean isFull() {
		return contents.quantity == getCapacity();
	}
}
