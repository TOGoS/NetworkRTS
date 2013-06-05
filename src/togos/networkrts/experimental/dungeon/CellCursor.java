package togos.networkrts.experimental.dungeon;


class CellCursor {
	Room room;
	float x, y, z;
	
	public CellCursor() { }
	
	public void set( Room room, float x, float y, float z ) {
		this.room = room;
		this.x = x; this.y = y; this.z = z;
	}
	
	public void set( CellCursor c ) { set( c.room, c.x, c.y, c.z ); }
		
	public void move( float dx, float dy, float dz ) {
		x += dx; y += dy; z += dz;
		if( room != null && !room.contains(x,y,z) ) {
			for( Room.Neighbor n : room.neighbors ) {
				if( n.contains(x,y,z) ) {
					this.room = n.room;
					this.x -= n.x;
					this.y -= n.y;
				}
			}
		}
	}
	
	public static int floor( float x ) { return (int)Math.floor(x); }
	
	public Block[] getAStack() {
		Block[] stack = getStack();
		return stack == null ? Block.EMPTY_STACK : stack;
	}
	
	public Block[] getStack() {
		if( room == null ) return null;
		return room.blockField.getStack( floor(x), floor(y), floor(z) );
	}

	public void addBlock( Block block ) {
		if( room == null ) return;
		room.blockField.addBlock( floor(x), floor(y), floor(z), block );
	}
	
	public void removeBlock( Block block ) {
		if( room == null ) return;
		room.blockField.removeBlock( floor(x), floor(y), floor(z), block );
	}
	
	public Block[] getStackAtZ(int zz) {
		if( room == null ) return null;
		return room.blockField.getStack( floor(x), floor(y), zz );
	}
}
