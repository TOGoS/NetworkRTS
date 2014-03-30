package togos.networkrts.experimental.game19.world.thing.pickup;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTileInternals;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.thing.SubstanceQuantity;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class SubstanceContainerInternals implements NonTileInternals<BlargNonTile>
{
	public final SubstanceContainerType type;
	public final SubstanceQuantity contents;
	
	public SubstanceContainerInternals( SubstanceContainerType type, SubstanceQuantity contents ) {
		this.type = type;
		this.contents = contents;
	}

	@Override public NonTile update(BlargNonTile nt, long time, World w, MessageSet messages, NonTileUpdateContext updateContext) {
		boolean pickedUp = false;
		for( Message m : messages ) {
			switch( m.type ) {
			case REQUEST_PICKUP:
				if( !pickedUp ) {
					updateContext.sendMessage(Message.create(m.sourceAddress, MessageType.INCOMING_ITEM, this));
					pickedUp = true;
				}
			}
		}
		return pickedUp ? null : nt;
	}
	
	@Override public Icon getIcon() {
		double contentVolume = contents.substance.unitVolume * contents.quantity;
		double fullness = type.internalVolume/contentVolume;
		int iconIdx = (int)Math.round(fullness * (type.icons.length-1));
		iconIdx = iconIdx < 0 ? 0 : iconIdx >= type.icons.length ? type.icons.length-1 : iconIdx;
		return type.icons[iconIdx];
	}
	@Override public AABB getRelativePhysicalAabb() { return type.relativePhysicalAabb; }
	@Override public long getNonTileAddressFlags() {
		return BitAddresses.PHYSINTERACT|BitAddresses.PICKUP;
	}
	@Override public long getNextAutoUpdateTime() { return Long.MAX_VALUE; }
}
