package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityAggregation;

public interface NonTile extends EntityAggregation, HasPositionAndVelocity
{
	// Note: For now, the implication of methods returning AABBs
	// is that those represent the nontile's physical bounds.
	// But AABBs might also be used for non-physical bounding boxes,
	// such as to represent the area a NonTile is monitoring,
	// and physical shape might be more complicated than a rectangle.
	// So I expect these methods to change.
	
	/**
	 * Return the bit address of the nontile itself.
	 * This will be between (inclusive) min and max addresses.
	 */
	public long getBitAddress();
	/** Return the nontile's bounding box relative to its x, y position */
	public AABB getRelativePhysicalAabb();
	/** Return the nontile's bounding box, taking its position into account */
	public AABB getAbsolutePhysicalAabb();
	public Icon getIcon();
	
	//// Move-ey stuff
	
	public NonTile withPositionAndVelocity(long time, double newX, double newY, double newVx, double newVy);
	
	//// Behavior-ey stuff
	
	/**
	 * Phase 1: update position+velocity, ignoring other Nontiles.
	 * Phase 2: handle collisions and messages.
	 * 
	 * It is the responsibility of the implementor of this method
	 * (rather than the delivery system)
	 * to ignore inapplicable messages and (messages aside) repeated
	 * updates for the same timestamp.
	 * 
	 * Ideally, the passage of time and reception of messages
	 * should be handled separately within this method.
	 * This will be called after withUpdatedPosision,
	 * so must use a separate internal timestamp to track 'last update'
	 * vs 'last position/velocity update'.
	 * 
	 * A naive delivery system could call update for every tick, passing all
	 * incoming messages for the entire system.  It might call update
	 * several times in one tick, and it might pass messages in in multiple
	 * batches.
	 */
	public NonTile update( long time, World w, MessageSet incomingMessages, NonTileUpdateContext updateContext );
}
