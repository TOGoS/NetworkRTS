package togos.networkrts.experimental.game19.world.thing;

import static togos.networkrts.experimental.game19.sim.Simulation.GRAVITY;
import static togos.networkrts.experimental.game19.sim.Simulation.SIMULATED_TICK_INTERVAL;
import togos.networkrts.experimental.game19.physics.BlockCollision;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.sim.Simulation;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.BlargNonTile;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTileInternals;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.AABB;

public abstract class AbstractPhysicalNonTileInternals implements NonTileInternals<BlargNonTile>
{
	protected BlargNonTile updatePhysics(BlargNonTile nt, long newTime, World world) {
		//double duration = newTime - referenceTime;
		//BlockCollision.findCollisionWithRst(this, world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID);
		double interval = Simulation.SIMULATED_TICK_INTERVAL * (newTime-nt.referenceTime);
		
		double newX = nt.x+nt.vx*interval;
		double newY = nt.y+nt.vy*interval;
		double newVx = nt.vx;
		double newVy = nt.vy;
		boolean onGround = false;
		
		AABB relAabb = this.getRelativePhysicalAabb();
		final BlockCollision c = BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX, newY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID);
		correctPosition: if( c != null ) {
			boolean xFirst;
			xFirst = Math.abs(c.correctionX) < Math.abs(c.correctionY);
			if( xFirst ) {
				if( BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX + c.correctionX, newY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID) == null ) {
					newX += c.correctionX;
					newVx *= -0.6;
					break correctPosition;
				}
				if( BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX, newY + c.correctionY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID) == null ) {
					newY += c.correctionY;
					newVy *= -0.6;
					break correctPosition;
				}
			} else {
				if( BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX, newY + c.correctionY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID) == null ) {
					newY += c.correctionY;
					newVy *= -0.6;
					break correctPosition;
				}
				if( BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX + c.correctionX, newY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID) == null ) {
					newX += c.correctionX;
					newVx *= -0.6;
					break correctPosition;
				}
			}
			
			// WHATEVER JUST GET THEM OUT
			newX += c.correctionX;
			newY += c.correctionY;
			newVx *= -0.6;
			newVy *= -0.6;
		}
		if( c != null && c.correctionY < 0 && Math.abs(newVy) < 0.5 ) {
			onGround = true;
		}
		
		boolean resting = false;
		if( onGround ) {
			newVy = 0;
			newVx *= 0.6 * (1-SIMULATED_TICK_INTERVAL);
			if( Math.abs(newVx) < 0.1 ) {
                newVx = 0;
				resting = true;
			}
		}
		
		return nt.withPositionAndVelocity(newTime, newX, newY, newVx, resting ? newVy : newVy + interval*GRAVITY);
	}
	
	@Override public BlargNonTile update(BlargNonTile nt, long time, World world, MessageSet messages, NonTileUpdateContext updateContext) {
		return updatePhysics(nt, time, world);
	}
}
