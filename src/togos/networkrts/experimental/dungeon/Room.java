package togos.networkrts.experimental.dungeon;

import java.util.ArrayList;
import java.util.List;

class Room {
	static class Neighbor {
		final Room room;
		final int x, y, z;
		
		public Neighbor( Room r, int x, int y, int z ) {
			this.room = r;
			this.x = x; this.y = y; this.z = z;
		}
		
		public boolean contains( float x, float y, float z ) {
			return this.room.contains( x - this.x, y - this.y, z - this.z );
		}
	}
	
	public long roomId;
	public BlockField blockField;
	public final List<Room.Neighbor> neighbors = new ArrayList();
	
	public Room( int w, int h, int d, Block[] fill ) {
		this.blockField = new BlockField(w, h, d, fill);
	}
	
	public boolean contains( float x, float y, float z ) {
		return
			x >= 0 && x <= blockField.w &&
			y >= 0 && y <= blockField.h &&
			z >= 0 && z <= blockField.d;
	}
	
	public int getWidth() { return blockField.w; }
	public int getHeight() { return blockField.h; }
	public int getDepth() { return blockField.d; }
}