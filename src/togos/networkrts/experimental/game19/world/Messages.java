package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityRange;

public class Messages
{
	public static MessageSet subsetApplicableTo(MessageSet s, EntityRange er) {
		// Save some work for the simple case
		if( s == MessageSet.EMPTY ) return s;
		
		AABB aabb = er.getAabb();
		return s.subsetApplicableTo(aabb.minX, aabb.minY, aabb.maxX, aabb.maxY, er.getMinBitAddress(), er.getMaxBitAddress());
	}
}
