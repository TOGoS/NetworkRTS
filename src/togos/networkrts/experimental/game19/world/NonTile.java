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
	
	/*
	 * Idea: explicit 2-phase update
	 * 
	 * Have a single update method that takes the phase as a parameter.
	 * 
	 * Have separate address flags for
	 * - needs time-based phase 1 update
	 * - needs time-based phase 2 update
	 * 
	 * Incoming messages are passed only to phase 2.
	 *  
	 * As an optimization, either phase will only result in update being called if:
	 *   the object's nextAutoUpdateTime <= upcoming tick AND the appropriate phase bit is set
	 *   OR
	 *   there are incoming messages to be handled by this update phase 
	 */
	
	/**
	 * Phase 1 update.
	 * 
	 * Update position based on velocity, ignoring
	 * the rest of the world.  When applicable,
	 * this will be called before update(...)
	 */
	public NonTile withUpdatedPosition(long time);
	
	/**
	 * Phase 2 update.
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
