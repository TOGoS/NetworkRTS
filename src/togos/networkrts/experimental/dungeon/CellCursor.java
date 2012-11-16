package togos.networkrts.experimental.dungeon;


class CellCursor {
	Room room;
	float x, y;
	
	public CellCursor() { }
	
	public void init( Room room, float x, float y ) {
		this.room = room;
		this.x = x; this.y = y;
	}
	
	public void move( float dx, float dy ) {
		x += dx; y += dy;
		if( room != null && !room.contains(x,y) ) {
			for( Room.Neighbor n : room.neighbors ) {
				if( n.contains(x,y) ) {
					this.room = n.room;
					this.x -= n.x;
					this.y -= n.y;
				}
			}
		}
	}
	
	public static int floor( float x ) { return (int)Math.floor(x); }
	
	public Block[] getStack() {
		if( room == null ) return null;
		return room.blockField.getStack( floor(x), floor(y) );
	}

	public void addBlock( Block block ) {
		if( room == null ) return;
		room.blockField.addBlock( floor(x), floor(y), block );
	}
	
	public void removeBlock( Block block ) {
		if( room == null ) return;
		room.blockField.removeBlock( floor(x), floor(y), block );
	}
}