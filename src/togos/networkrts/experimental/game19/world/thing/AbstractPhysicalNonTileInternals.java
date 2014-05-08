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
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTileInternals;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityRanges;
import togos.networkrts.experimental.gameengine1.index.Visitor;

public abstract class AbstractPhysicalNonTileInternals implements NonTileInternals<BlargNonTile>
{
	protected static class PhysicsResult {
		public final BlargNonTile nt;
		public final double collisionSpeed;
		
		public PhysicsResult( BlargNonTile nt, double collisionSpeed ) {
			this.nt = nt;
			this.collisionSpeed = collisionSpeed;
		}
	}
	
	protected PhysicsResult updatePhysics(final BlargNonTile nt, long newTime, World world) {
		//double duration = newTime - referenceTime;
		//BlockCollision.findCollisionWithRst(this, world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID);
		double interval = Simulation.SIMULATED_TICK_INTERVAL * (newTime-nt.referenceTime);
		
		// TODO: more accurately find point of collision by ray-casting from origin
		
		double newX = nt.x+nt.vx*interval;
		double newY = nt.y+nt.vy*interval;
		//double newVx = nt.vx;
		//double newVy = nt.vy;
		double collisionSpeed = 0;
		boolean onGround = false;
		
		// Handle NonTile-NonTile collisions
		// Less massive NonTile (or the one farther up or to the right
		//   if they have the same mass) will adjust its position.
		//   If position corrections conflict, those from the more massive
		//     NonTile take precedence
		// Both will update their velocity
		// Block collisions are handled afterwards, as they
		//   are the ultimate authority on position correction
		
		class InterNonTileForceAggregator implements Visitor<NonTile> {
			public double xCorrection, yCorrection;
			protected double xCorrectionPeerMass, yCorrectionPeerMass;
			public double newVx, newVy;
			
			public InterNonTileForceAggregator(NonTile nt) {
				newVx = nt.getVelocityX();
				newVy = nt.getVelocityY();
			}
			
			protected double minAbs( double a, double b ) {
				return Math.abs(a) < Math.abs(b) ? a : b;
			}
			
			/**
			 * Figure how much a should be moved to not overlap with b
			 */
			protected double correction( double amin, double amax, double bmin, double bmax ) {
				return minAbs(
					bmin - amax < 0 ? bmin - amax : 0,
					bmax - amin > 0 ? bmax - amin : 0
				);
			}
			
			@Override public void visit(NonTile ntb) {
				if( ntb == nt ) return;
				
				double ma = 1;
				double mb = 1;
				AABB a = nt.getAabb();
				AABB b = ntb.getAabb();
				
				boolean smaller =
					ma < mb ||
					ma == mb && (
						a.minY < b.minY ||
						a.minY == b.minY && a.minX < b.minX );
				
				double cx = correction(a.minX, a.maxX, b.minX, b.maxX);
				double cy = correction(a.minY, a.maxY, b.minY, b.maxY);
				if( cx != 0 && cy != 0 ) {
					// Make the bigger one 0
					if( Math.abs(cx) > Math.abs(cy) ) {
						cx = 0;
					} else {
						cy = 0;
					}
				}
				if( smaller )  {
					if( Math.abs(cx) > Math.abs(xCorrection) ) {
						xCorrection = cx;
					}
					if( Math.abs(cy) > Math.abs(yCorrection) ) {
						yCorrection = cy;
					}
				}
				if( cx != 0 ) {
					newVx = (nt.vx*(ma - mb) + 2*mb*ntb.getVelocityX())/(ma+mb);
				}
				if( cy != 0 ) {
					newVy = (nt.vy*(ma - mb) + 2*mb*ntb.getVelocityY())/(ma+mb);
				}
				
				// TODO: Force should be proportional to overlap, I guess
				
				/*
				double vx = nt.getX()-v.getX();
				double vy = nt.getY()-v.getY();
				
				if( vx == 0 && vy == 0 ) return;
				
				double rat = 1/(vx*vx + vy*vy);
				fx += rat * vx;
				fy += rat * vy;
				*/
			}
		}
		// Excessive object allocation happens here
		InterNonTileForceAggregator fa = new InterNonTileForceAggregator(nt);
		world.nonTiles.forEachEntity(
			EntityRanges.create(
				nt.getAabb(), BitAddresses.TYPE_NONTILE|BitAddresses.RIGIDBODY, BitAddresses.maxForType(BitAddresses.TYPE_NONTILE) 
			), fa);
		newX += fa.xCorrection;
		newY += fa.yCorrection;
		double newVx = fa.newVx;
		double newVy = fa.newVy;
		
		AABB relAabb = this.getRelativePhysicalAabb();
		final BlockCollision c = BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX, newY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID);
		correctPosition: if( c != null ) {
			boolean xFirst;
			xFirst = Math.abs(c.correctionX) < Math.abs(c.correctionY);
			if( xFirst ) {
				if( BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX + c.correctionX, newY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID) == null ) {
					collisionSpeed = nt.vx;
					newX += c.correctionX;
					newVx *= -0.6;
					break correctPosition;
				}
				if( BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX, newY + c.correctionY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID) == null ) {
					collisionSpeed = nt.vy;
					newY += c.correctionY;
					newVy *= -0.6;
					break correctPosition;
				}
			} else {
				if( BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX, newY + c.correctionY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID) == null ) {
					collisionSpeed = nt.vy;
					newY += c.correctionY;
					newVy *= -0.6;
					break correctPosition;
				}
				if( BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX + c.correctionX, newY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID) == null ) {
					collisionSpeed = nt.vx;
					newX += c.correctionX;
					newVx *= -0.6;
					break correctPosition;
				}
			}
			
			// WHATEVER JUST GET THEM OUT
			collisionSpeed = nt.vx + nt.vy;
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
		
		return new PhysicsResult(
			nt.withPositionAndVelocity(newTime, newX, newY, newVx, resting ? newVy : newVy + interval*GRAVITY),
			collisionSpeed
		);
	}
	
	@Override public BlargNonTile update(BlargNonTile nt, long time, World world, MessageSet messages, NonTileUpdateContext updateContext) {
		return updatePhysics(nt, time, world).nt;
	}
}
