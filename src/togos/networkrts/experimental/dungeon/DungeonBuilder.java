package togos.networkrts.experimental.dungeon;

public class DungeonBuilder
{
	public Room currentRoom;
	
	public Room makeRoom( int w, int h, Block[][] stacks, int[] tiles ) {
		Room r = new Room(w, h);
		for( int i=0; i<tiles.length; ++i ) {
			r.blockField.blockStacks[i] = stacks[tiles[i]];
		}
		return currentRoom = r;
	}
	
	public Room makeRoom( int w, int h ) {
		Room r = new Room(w, h);
		r.blockField.fill( Block.WALL.stack );
		for( int y=1; y<h-1; ++y ) {
			for( int x=1; x<w-1; ++x ) {
				r.blockField.setStack( x, y, Block.FLOOR.stack );
			}
		}
		return currentRoom = r;
	}

	public void link( Room r0, Room r1, int dx, int dy ) {
		r0.neighbors.add( new Room.Neighbor(r1,  dx,  dy) );
		r1.neighbors.add( new Room.Neighbor(r0, -dx, -dy) );
	}
	
	public void connectSides( Room r0, Room r1, int dx, int dy ) {
		link( r0, r1, dx >= 0 ? dx*r0.getWidth() : dx*r1.getWidth(), dy >= 0 ? dy*r0.getHeight() : dy*r1.getHeight() );
		if( dx != 0 ) {
			if( dx < 0 ) {  Room t = r0; r0 = r1; r1 = t; dx = -dx; }
			int minHeight = Math.min(r0.getHeight(), r1.getHeight() );
			for( int y=1; y<minHeight-1; ++y ) {
				r0.blockField.setStack( r0.getWidth()-1, y, Block.FLOOR.stack );
				r1.blockField.setStack( 0, y, Block.FLOOR.stack );
			}
		} else {
			if( dy < 0 ) {  Room t = r0; r0 = r1; r1 = t; dy = -dy; }
			int minWidth = Math.min(r0.getWidth(), r1.getWidth() );
			for( int x=1; x<minWidth-1; ++x ) {
				r0.blockField.setStack( x, r0.getHeight()-1, Block.FLOOR.stack );
				r1.blockField.setStack( x, 0, Block.FLOOR.stack );
			}
		}
	}
	
	public Room dig( int dx, int dy ) {
		Room oldRoom = currentRoom;
		Room newRoom = makeRoom( oldRoom.getWidth(), oldRoom.getHeight() );
		connectSides( oldRoom, newRoom, dx, dy );
		return currentRoom = newRoom;
	}
	
	public Room north() { return dig(0,-1); }
	public Room south() { return dig(0,+1); }
	public Room east( ) { return dig(+1,0); }
	public Room west( ) { return dig(-1,0); }
	
	public void digTo( int dx, int dy, Room target ) {
		connectSides( currentRoom, target, dx, dy );
	}

	public void northTo( Room target ) { digTo(0,-1,target); }
	public void southTo( Room target ) { digTo(0,+1,target); }
	public void eastTo(  Room target ) { digTo(-1,0,target); }
	public void westTo(  Room target ) { digTo(+1,0,target); }
}
