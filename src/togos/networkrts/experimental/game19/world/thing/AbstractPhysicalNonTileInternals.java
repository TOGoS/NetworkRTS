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
import togos.networkrts.experimental.game19.world.PositionInWorld;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.RSTNode.NodeType;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityRanges;
import togos.networkrts.experimental.gameengine1.index.Visitor;
import togos.networkrts.util.BitAddressUtil;

public abstract class AbstractPhysicalNonTileInternals implements NonTileInternals<BlargNonTile>
{
	protected static class PhysicsResult {
		public final BlargNonTile nt;
		public final double collisionVx, collisionVy;
		
		public PhysicsResult( BlargNonTile nt, double cvx, double cvy ) {
			this.nt = nt;
			this.collisionVx = cvx;
			this.collisionVy = cvy;
		}

		public double getCollisionSpeed() {
			return Math.sqrt(collisionVx*collisionVx+collisionVy*collisionVy);
		}
	}
	
	protected PositionInWorld findNearestSolidIntersectionAlongPath( double x0, double y0, double x1, double y1, RSTNode rst, int rstX0, int rstY0, int rstSizePower ) {
		// TODO: Change to use a SOLID flag,
		// which should used by both nontiles (instead of RIGIDBODY)
		// and blocks
		if( BitAddressUtil.rangesIntersect(rst, BitAddresses.PHYSINTERACT, BitAddressUtil.MAX_ADDRESS ) ) {
			int size = 1<<rstSizePower;
			int rstX1 = rstX0 + size;
			int rstY1 = rstY0 + size;
			
			// Put x0, y0 at the first intersection with this square
			
			if(
				(x1 < rstX0 && x0 < rstX0) ||
				(x1 > rstX1 && x0 > rstX1) ||
				(y1 < rstY0 && y0 < rstY0) ||
				(y1 > rstY1 && y0 > rstY1)
			) return null;
			
			narrowX: {
				double xEdge0, xEdge1;
				if( x1 > x0 ) {
					xEdge0 = Math.max(x0, rstX0);
					xEdge1 = Math.min(x1, rstX1);
				} else if( x0 > x1 ) {
					xEdge0 = Math.min(x0, rstX1);
					xEdge1 = Math.max(x1, rstX0);
				} else {
					break narrowX;
				}
				double ydist = y1 - y0;
				y1 = y0 + ydist * (xEdge1-x0)/(x1-x0);
				y0 = y0 + ydist * (xEdge0-x0)/(x1-x0);
				
				if(
					(y1 < rstY0 && y0 < rstY0) ||
					(y1 > rstY1 && y0 > rstY1)
				) return null;
				
				x0 = xEdge0; x1 = xEdge1;
			}
			
			narrowY: {
				double yEdge0, yEdge1;
				if( y1 > y0 ) {
					yEdge0 = Math.max(y0, rstY0);
					yEdge1 = Math.min(y1, rstY1);
				} else if( y0 > y1 ) {
					yEdge0 = Math.min(y0, rstY1);
					yEdge1 = Math.max(y1, rstY0);
				} else {
					break narrowY;
				}
				double xdist = x1 - x0;
				x1 = x0 + xdist * (yEdge1-y0)/(y1-y0);
				x0 = x0 + xdist * (yEdge0-y0)/(y1-y0);
				
				if(
					(x1 < rstX0 && x0 < rstX0) ||
					(x1 > rstX1 && x0 > rstX1)
				) return null;
				
				y0 = yEdge0; y1 = yEdge1;
			}
			
			if( rst.getNodeType() == NodeType.BLOCKSTACK ) {
				return new PositionInWorld(x0, y0, 0);
			} else {
				int subSizePower = rstSizePower-1;
				int subSize = 1<<subSizePower;
				RSTNode[] subNodes = rst.getSubNodes();
				
				int subx0, subx1, suby0, suby1;
				if( x1 - x0 > 0 ) { subx0 = 0; subx1 = 1; }
				else { subx0 = 1; subx1 = 0; }
				if( y1 - y0 > 0 ) { suby0 = 0; suby1 = 1; }
				else { suby0 = 1; suby1 = 0; }
				
				PositionInWorld p;
				if( (p = findNearestSolidIntersectionAlongPath(x0, y0, x1, y1, subNodes[subx0+suby0*2], rstX0+subx0*subSize, rstY0+suby0*subSize, subSizePower)) != null ) return p;
				if( (p = findNearestSolidIntersectionAlongPath(x0, y0, x1, y1, subNodes[subx1+suby0*2], rstX0+subx1*subSize, rstY0+suby0*subSize, subSizePower)) != null ) return p;
				if( (p = findNearestSolidIntersectionAlongPath(x0, y0, x1, y1, subNodes[subx0+suby1*2], rstX0+subx0*subSize, rstY0+suby1*subSize, subSizePower)) != null ) return p;
				if( (p = findNearestSolidIntersectionAlongPath(x0, y0, x1, y1, subNodes[subx1+suby1*2], rstX0+subx1*subSize, rstY0+suby1*subSize, subSizePower)) != null ) return p;
			}
		}
		
		return null;
	}
	
	protected PositionInWorld findNearestSolidIntersectionAlongPath( double x0, double y0, double x1, double y1, World world ) {
		int rad = 1<<(world.rstSizePower-1);
		// TODO: Also detect intersections with other solid nontiles
		return findNearestSolidIntersectionAlongPath( x0, y0, x1, y1, world.rst, -rad, -rad, world.rstSizePower );
	}
	
	protected PhysicsResult updatePhysics(final BlargNonTile nt, long newTime, World world) {
		assert nt.referenceTime < newTime;
		
		// Handle NonTile-NonTile collisions
		// Less massive NonTile (or the one farther up or to the right
		//   if they have the same mass) will adjust its position.
		//   If position corrections conflict, those from the more massive
		//     NonTile take precedence
		// Both will update their velocity
		// Block collisions are handled afterwards, as they
		//   are the ultimate authority on position correction
		
		class InterNonTileForceAggregator implements Visitor<NonTile> {
			public double correctionX, correctionY;
			protected NonTile collideeX, collideeY;
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
				if( true /*smaller*/ )  {
					if( Math.abs(cx) > Math.abs(correctionX) ) {
						correctionX = cx;
					}
					if( Math.abs(cy) > Math.abs(correctionY) ) {
						correctionY = cy;
					}
				}
				
				// Fake some friction
				double ofx, ofy, bfx, bfy;
				if( cx != 0 ) {
					collideeX = ntb;
					bfx = 0.6; ofx = 0.0;
					bfy = 0.1; ofy = 0.9; 
				} else if( cy != 0 ) {
					collideeY = ntb;
					bfy = 0.6; ofy = 0.0;
					bfx = 0.1; ofx = 0.9; 
				} else {
					return;
				}
				newVx = newVx*ofx + bfx*(nt.vx*(ma - mb) + 2*mb*ntb.getVelocityX())/(ma+mb);
				newVy = newVy*ofy + bfy*(nt.vy*(ma - mb) + 2*mb*ntb.getVelocityY())/(ma+mb);
				double meanVx = (ntb.getVelocityX()+newVx)/2;
				double meanVy = (ntb.getVelocityY()+newVy)/2;
				if( Math.abs(newVx - meanVx) < 0.1 ) newVx = meanVx;
				if( Math.abs(newVy - meanVy) < 0.1 ) newVy = meanVy;
			}
		}
		
		//double duration = newTime - referenceTime;
		//BlockCollision.findCollisionWithRst(this, world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID);
		double interval = Simulation.SIMULATED_TICK_INTERVAL * (newTime-nt.referenceTime);
		assert interval != 0;
		
		// TODO: more accurately find point of collision by ray-casting from origin
		
		//double newX = nt.x+nt.vx*interval;
		//double newY = nt.y+nt.vy*interval;
		//double newVx = nt.vx;
		//double newVy = nt.vy;
		double collisionVx = 0, collisionVy = 0;
		boolean onGround = false;
		
		final AABB relAabb = this.getRelativePhysicalAabb();
		
		// Step until there's a collision
		double newX = nt.x, newY = nt.y;
		
		boolean findCollisionsALessSlowWay = true;
		boolean findCollisionsAReallySlowWay = false;
		
		if( findCollisionsALessSlowWay ) {
			PositionInWorld newPos = findNearestSolidIntersectionAlongPath( newX, newY, newX+nt.vx*interval, newY+nt.vy*interval, world );
			if( newPos != null ) {
				newX = newPos.x;
				newY = newPos.y;
			} else {
				newX += nt.vx*interval;
				newY += nt.vy*interval;
			}
		} else if( findCollisionsAReallySlowWay ) {
			double stepDx = nt.vx;
			double stepDy = nt.vy;
			double stepInterval = interval;
			double width = relAabb.maxX - relAabb.minX;
			double height = relAabb.maxY - relAabb.minY;
			if( stepDx > width ) {
				stepInterval *= width/stepDx;
			} else if( stepDx < -width ) {
				stepInterval *= -width/stepDx;
			}
			if( stepDy > width ) {
				stepInterval *= height/stepDy;
			} else if( stepDy < -height ) {
				stepInterval *= -height/stepDy;
			}
			
			assert stepInterval != 0;
			
			double steppedInterval = 0;
			findCollision: while( steppedInterval < interval ) {
				double dt = Math.min(interval - steppedInterval, stepInterval);
				newX += stepDx * dt;
				newY += stepDy * dt;
				steppedInterval += stepInterval; // Not dt!
				BlockCollision c = BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX, newY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID);
				if( c != null ) {
					break findCollision;
				}
			}
		} else {
			newX += nt.vx*interval;
			newY += nt.vy*interval;
		}
		
		double newVx, newVy;
		// RIGIDBODY indicates that it physically interacts
		// with other NonTiles.  Without it, skip detection
		// of collisions with them.
		if( (nt.getBitAddress() & BitAddresses.RIGIDBODY) != 0 ) {
			// Excessive object allocation happens here
			InterNonTileForceAggregator fa = new InterNonTileForceAggregator(nt);
			world.nonTiles.forEachEntity(
				EntityRanges.create(
					nt.getAabb(), BitAddresses.TYPE_NONTILE|BitAddresses.RIGIDBODY, BitAddresses.maxForType(BitAddresses.TYPE_NONTILE) 
				), fa);
			newX += fa.correctionX;
			newY += fa.correctionY;
			newVx = fa.newVx;
			newVy = fa.newVy;
		} else {
			newVx = nt.vx;
			newVy = nt.vy;
		}
		
		final BlockCollision c = BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX, newY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID);
		correctPosition: if( c != null ) {
			boolean xFirst;
			xFirst = Math.abs(c.correctionX) < Math.abs(c.correctionY);
			if( xFirst ) {
				if( BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX + c.correctionX, newY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID) == null ) {
					collisionVx = nt.vx;
					newX += c.correctionX;
					newVx *= -0.6;
					break correctPosition;
				}
				if( BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX, newY + c.correctionY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID) == null ) {
					collisionVy = nt.vy;
					newY += c.correctionY;
					newVy *= -0.6;
					break correctPosition;
				}
			} else {
				if( BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX, newY + c.correctionY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID) == null ) {
					collisionVy = nt.vy;
					newY += c.correctionY;
					newVy *= -0.6;
					break correctPosition;
				}
				if( BlockCollision.findCollisionWithRst(relAabb.shiftedBy(newX + c.correctionX, newY, 0), world, BitAddresses.PHYSINTERACT, Block.FLAG_SOLID) == null ) {
					collisionVx = nt.vx;
					newX += c.correctionX;
					newVx *= -0.6;
					break correctPosition;
				}
			}
			
			// WHATEVER JUST GET THEM OUT
			collisionVx = nt.vx;
			collisionVy = nt.vy;
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
			collisionVx,
			collisionVy
		);
	}
	
	@Override public BlargNonTile update(BlargNonTile nt, long time, World world, MessageSet messages, NonTileUpdateContext updateContext) {
		if( time == nt.referenceTime ) return nt;
		return updatePhysics(nt, time, world).nt;
	}
}
