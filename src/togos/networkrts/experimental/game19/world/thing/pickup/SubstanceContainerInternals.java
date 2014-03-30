package togos.networkrts.experimental.game19.world.thing.pickup;

import togos.networkrts.experimental.game19.physics.BlockCollision;
import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTileInternals;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.thing.Substance;
import togos.networkrts.experimental.game19.world.thing.SubstanceQuantity;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManInternals;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class SubstanceContainerInternals implements NonTileInternals<BlargNonTile>
{
	public final SubstanceContainerType type;
	public final SubstanceQuantity contents;
	
	public SubstanceContainerInternals( SubstanceContainerType type, SubstanceQuantity contents ) {
		this.type = type;
		this.contents = contents;
	}
	
	public static SubstanceContainerInternals filled( SubstanceContainerType type, Substance filler ) {
		return new SubstanceContainerInternals(type, new SubstanceQuantity(filler, type.getCapacity(filler)));
	}

	@Override public NonTile update(BlargNonTile nt, long time, World world, MessageSet messages, NonTileUpdateContext updateContext) {
		boolean pickedUp = false;
		for( Message m : messages ) {
			switch( m.type ) {
			case REQUEST_PICKUP:
				if( !pickedUp ) {
					updateContext.sendMessage(Message.create(m.sourceAddress, MessageType.INCOMING_ITEM, this));
					pickedUp = true;
				}
			default: // Ignore everything else
			}
		}
		if( pickedUp ) return null;
		
		double newX = nt.x, newY = nt.y, newVx = nt.vx, newVy = nt.vy;
		
		BlockCollision c = BlockCollision.findCollisionWithRst(nt, world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID);
		boolean onGround = false;
		if( c != null ) {
			if( c.correctionX != 0 && Math.abs(c.correctionX) < Math.abs(c.correctionY) ) {
				newX += c.correctionX;
				newVx *= -0.5;
			} else {
				newY += c.correctionY;
				newVy *= -0.5;
				if( c.correctionY < 0 && Math.abs(newVy) < 0.1 ) {
					newVy = 0;
					newVx *= 0.9;
					if( Math.abs(newVx) < 0.1 ) newVx = 0;
					onGround = true;
				}
			}
		}
		
		if( !onGround ) newVy += JetManInternals.GRAVITY;
		nt = nt.withPositionAndVelocity(time, newX, newY, newVx, newVy);
		
		return nt;
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

	public double getCapacity() {
		return type.getCapacity(contents.substance);
	}

	public SubstanceContainerInternals add(double delta) {
		return new SubstanceContainerInternals(type, new SubstanceQuantity(contents.substance, contents.quantity + delta));
	}
}
