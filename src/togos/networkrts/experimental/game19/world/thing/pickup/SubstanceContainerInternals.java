package togos.networkrts.experimental.game19.world.thing.pickup;

import static togos.networkrts.experimental.game19.sim.Simulation.GRAVITY;
import static togos.networkrts.experimental.game19.sim.Simulation.SIMULATED_TICK_INTERVAL;
import togos.networkrts.experimental.game19.physics.BlockCollision;
import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTileInternals;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.thing.Substance;
import togos.networkrts.experimental.game19.world.thing.SubstanceQuantity;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class SubstanceContainerInternals implements NonTileInternals<BlargNonTile>
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
	
	@Override public NonTile update(BlargNonTile nt, long time, World world, MessageSet messages, NonTileUpdateContext updateContext) {
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
		
		double newX = nt.x, newY = nt.y, newVx = nt.vx, newVy = nt.vy;
		
		if( nextAutoUpdateTime <= time ) {
			BlockCollision c = BlockCollision.findCollisionWithRst(nt, world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID);
			boolean onGround = false;
			if( c != null ) {
				if( c.correctionX != 0 && Math.abs(c.correctionX) < Math.abs(c.correctionY) ) {
					newX += c.correctionX;
					newVx *= -0.5 * SIMULATED_TICK_INTERVAL;
				} else {
					newY += c.correctionY;
					newVy *= -0.5 * SIMULATED_TICK_INTERVAL;
					if( c.correctionY < 0 && Math.abs(newVy) < 0.1 ) {
						newVy = 0;
						newVx *= 0.9;
						if( Math.abs(newVx) < 0.5 ) newVx = 0;
						onGround = true;
					}
				}
			}
			
			if( !onGround ) newVy += GRAVITY * SIMULATED_TICK_INTERVAL;
			nt = nt.withPositionAndVelocity(time, newX, newY, newVx, newVy).withInternals(withNextAutoUpdateTime(onGround ? Long.MAX_VALUE : time+1));
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
			BitAddresses.PICKUP |
			BitAddresses.UPPHASE2;
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
