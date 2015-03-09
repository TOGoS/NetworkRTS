package togos.networkrts.experimental.dungeon;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

class Projection implements Serializable, Cloneable
{
	private static final long serialVersionUID = 2920724968130479539L;
	
	public final BlockField blockField;
	public final Set<Room> roomsIncluded;
	/** Position of the origin within the block field */
	float originX, originY;
	
	public Projection( BlockField blockField, float originX, float originY, Set<Room> roomsIncluded ) {
		this.blockField = blockField;
		this.originX = originX;
		this.originY = originY;
		this.roomsIncluded = roomsIncluded;
	}
	public Projection( int w, int h, int d ) {
		this( new BlockField( w, h, d, SimpleBlock.EMPTY_STACK ), 0, 0, new HashSet<Room>() );
	}
	
	public void clear() {
		roomsIncluded.clear();
		blockField.clear();
	}
	
	public void projectFrom( Room r, float x, float y, float z ) {
		clear();
		Raycast.raycastXY( r, x, y, (int)z, blockField, blockField.w/2, blockField.h/2, roomsIncluded );
		this.originX = blockField.w/2f;
		this.originY = blockField.h/2f;
	}
	
	public void projectFrom(CellCursor pos) {
		projectFrom(pos.room, pos.x, pos.y, (int)pos.z);
	}
	
	public Projection clone() {
		return new Projection( blockField.clone(), originX, originY, new HashSet<Room>(roomsIncluded) );
	}
}
