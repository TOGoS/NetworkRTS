package togos.networkrts.experimental.dungeon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import togos.networkrts.experimental.dungeon.DungeonGame.Container;
import togos.networkrts.experimental.dungeon.DungeonGame.GameObject;
import togos.networkrts.experimental.dungeon.DungeonGame.Location;

class Room implements Container {
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
	public final List<Room.Neighbor> neighbors = new ArrayList<Room.Neighbor>();
	public final Set<RoomWatcher> watchers = new HashSet<RoomWatcher>();
	protected final HashSet<GameObject> containedGameObjects = new HashSet<GameObject>();
	
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
	
	public void updated() {
		for( RoomWatcher w : watchers ) w.roomUpdated(this);
	}
	
	public static int floor( float x ) { return (int)Math.floor(x); }
	
	@Override public void removeItem(GameObject obj) {
		Location l = obj.getLocation();
		blockField.removeBlock( floor(l.x), floor(l.y), floor(l.z), obj );
		containedGameObjects.remove(obj);
		updated();
	}
	
	@Override public void addItem(GameObject obj) {
		Location l = obj.getLocation();
		blockField.addBlock( floor(l.x), floor(l.y), floor(l.z), obj );
		containedGameObjects.add(obj);
		updated();
	}

	@Override public boolean canHold(GameObject obj) { return true; }

	@Override public Set<GameObject> getContents() {
		return containedGameObjects;
	}
}
