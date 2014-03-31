package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntityRange;

public class Messages
{
	public static MessageSet subsetApplicableTo(MessageSet s, EntityRange er) {
		if( s.size() == 0 ) return s;
		
		AABB aabb = er.getAabb();
		return s.subsetApplicableTo(aabb.minX, aabb.minY, aabb.maxX, aabb.maxY, er.getMinBitAddress(), er.getMaxBitAddress());
	}
	
	public static boolean isApplicableTo(MessageSet s, EntityRange er) {
		if( s.size() != 0 ) for( Message m : s ) {
			if( m.isApplicableTo(er) ) return true;
		}
		return false;
	}
	
	public static MessageSet union( MessageSet a, MessageSet b ) {
		if( b.size() == 0 ) return a;
		if( a.size() == 0 ) return b;
		ArrayMessageSet ams = new ArrayMessageSet();
		for( Message m : a ) ams.add(m);
		for( Message m : b ) ams.add(m);
		return ams;
	}
}
