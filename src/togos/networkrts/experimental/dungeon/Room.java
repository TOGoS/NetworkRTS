package togos.networkrts.experimental.dungeon;

import java.util.ArrayList;
import java.util.List;

class Room {
	static class Neighbor {
		final Room room;
		final int x, y;
		
		public Neighbor( Room r, int x, int y ) {
			this.room = r;
			this.x = x; this.y = y;
		}
		
		public boolean contains( float x, float y ) {
			return this.room.contains( x - this.x, y - this.y );
		}
	}
	
	public BlockField blockField;
	public final List<Room.Neighbor> neighbors = new ArrayList();
	
	public Room( int w, int h ) {
		this.blockField = new BlockField(w,h);
	}
	
	public boolean contains( float x, float y ) {
		return x >= 0 && y >= 0 && x <= blockField.w && y <= blockField.h;
	}
	
	public int getWidth() { return blockField.w; }
	public int getHeight() { return blockField.h; }
}