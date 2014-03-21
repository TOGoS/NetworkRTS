package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityRange;

public interface NonTile extends EntityRange, HasPositionAndVelocity
{
	// Note: For now, the implication of methods returning AABBs
	// is that those represent the nontile's physical bounds.
	// But AABBs might also be used for non-physical bounding boxes,
	// such as to represent the area a NonTile is monitoring,
	// and physical shape might be more complicated than a rectangle.
	// So I expect these methods to change.
	
	/** Return the nontile's bounding box relative to its x, y position */
	public AABB getRelativePhysicalAabb();
	/** Return the nontile's bounding box, taking its position into account */
	public AABB getAbsolutePhysicalAabb();
	public Icon getIcon();

	//// Move-ey stuff
	
	public NonTile withPositionAndVelocity(long time, double newX, double newY, double newVx, double newVy);
	
	//// Behavior-ey stuff
	
	public NonTile withUpdatedPosition(long time);
	public NonTile update( long time, World w, MessageSet incomingMessages, NonTileUpdateContext updateContext );
}
