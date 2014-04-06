package togos.networkrts.experimental.game18.sim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import togos.networkrts.experimental.game18.StorageContext;
import togos.networkrts.experimental.qt2drender.ImageHandle;
import togos.networkrts.experimental.qt2drender.Sprite;
import togos.networkrts.experimental.qt2drender.VizState;
import togos.networkrts.experimental.qt2drender.VizState.BackgroundLink;
import togos.networkrts.util.BitAddressUtil;
import togos.networkrts.util.SoftResourceHandle;
import togos.networkrts.util.StorageUtil;

public class Room implements SimNode
{
	public static class Neighbor {
		public final long roomId;
		public final int offsetX, offsetY, offsetZ, width, height, depth;
		
		public Neighbor( long roomId, int offsetX, int offsetY, int offsetZ, int width, int height, int depth ) {
			this.roomId = roomId;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
			this.width   = width;
			this.height  = height;
			this.depth   = depth;
		}
	}
	
	interface ThingBehavior<Thing> {
		public long getNextAutoUpdateTime( Thing t );
		public Thing update( Thing t, SimNode rootNode, long timestamp, Message m, List<Message> messageQueue );
	}
	
	public static class BoringestThingBehavior<Thing> implements ThingBehavior<Thing> {
		public static final BoringestThingBehavior<?> instance = new BoringestThingBehavior<Object>();
		@SuppressWarnings("unchecked")
		public static final <Thing> BoringestThingBehavior<Thing> getInstance() {
			return (BoringestThingBehavior<Thing>)instance;
		}
		
		private BoringestThingBehavior() { }
		
		public long getNextAutoUpdateTime( Thing t ) {
			return Long.MAX_VALUE;
		}
		public Thing update( Thing t, SimNode rootNode, long timestamp, Message m, List<Message> messageQueue ) {
			return t;
		}
	}
	
	public static class Tile implements SimNode {
		final long id;
		final String imageUri;
		// TODO: Eventually tiles should have much more complex physics and behavior
		// - pipes
		// - breakable blocks
		// - slippery blocks
		// - stairs
		// - ramps
		// - be able to have any behavior a sprite can have
		final boolean isSolid;
		final boolean isOpaque;
		public final ThingBehavior<Tile> behavior;
		
		public final Tile[] single = new Tile[] { this };
		
		public Tile( long id, String imageUri, boolean isSolid, boolean isOpaque, ThingBehavior<Tile> behavior ) {
			this.id = id;
			this.imageUri = imageUri;
			this.isSolid = isSolid;
			this.isOpaque = isOpaque;
			this.behavior = behavior;
		}

		@Override public long getMinId() {
			return id == BitAddressUtil.NO_ADDRESS ? BitAddressUtil.MAX_ADDRESS : id;
		}
		@Override public long getMaxId() {
			return id == BitAddressUtil.NO_ADDRESS ? BitAddressUtil.MIN_ADDRESS : id;
		}
		@Override public long getNextAutoUpdateTime() {
			return behavior.getNextAutoUpdateTime(this);
		}
		@Override public Tile update( SimNode rootNode, long timestamp, Message m, List<Message> messageQueue ) {
			return behavior.update(this, rootNode, timestamp, m, messageQueue);
		}
		@Override public <T> T get( long id, Class<T> expectedClass ) {
			return (id == this.id) ? expectedClass.cast(this) : null;
		}
	}
	
	public static class DynamicThing implements SimNode {
		public final long id;
		public final float x, y, z;
		// TODO: I suppose I would eventually like non-square things...
		public final float width, height;
		public final String imageUri;
		public final float imageWidth, imageHeight;
		public final ThingBehavior<DynamicThing> behavior;
		
		public DynamicThing( long id,
			float x, float y, float z, float width, float height,
			String imageUri, float imageWidth, float imageHeight,
			ThingBehavior<DynamicThing> behavior
		) {
			this.id = id;
			this.x = x; this.y = y; this.z = z;
			this.width = width; this.height = height;
			this.imageUri = imageUri; this.imageWidth = imageWidth; this.imageHeight = imageHeight;
			this.behavior = behavior;
		}

		@Override public long getMinId() {
			return id == BitAddressUtil.NO_ADDRESS ? BitAddressUtil.MAX_ADDRESS : id;
		}
		@Override public long getMaxId() {
			return id == BitAddressUtil.NO_ADDRESS ? BitAddressUtil.MIN_ADDRESS : id;
		}
		@Override public long getNextAutoUpdateTime() {
			return behavior.getNextAutoUpdateTime(this);
		}
		@Override public DynamicThing update( SimNode rootNode, long timestamp, Message m, List<Message> messageQueue ) {
			return behavior.update(this, rootNode, timestamp, m, messageQueue);
		}
		@Override public <T> T get( long id, Class<T> expectedClass ) {
			return (id == this.id) ? expectedClass.cast(this) : null;
		}
	}
	
	public final long id;
	public final int width, height, depth;
	public final int originX, originY;
	public final Neighbor[] neighbors;
	public final BackgroundLink backgroundLink;
	public final Tile[][] tiles;
	public final DynamicThing[] dynamicThings;
	
	protected final long minId, maxId;
	protected final boolean anyTilesHaveInterestingBehavior;
	protected final long nextAutoUpdateTime;
	
	public Room(
		long id, int width, int height, int depth, int originX, int originY,
		Neighbor[] neighbors, BackgroundLink backgroundLink, Tile[][] tiles, DynamicThing[] dynamicThings
	) {
		this.id = id;
		this.width = width; this.height = height; this.depth = depth;
		this.originX = originX; this.originY = originY;
		this.neighbors = neighbors;
		this.backgroundLink = backgroundLink;
		this.tiles = tiles; this.dynamicThings = dynamicThings;
		
		long nextAutoUpdateTime = Long.MAX_VALUE;
		long minId = BitAddressUtil.toMinAddress(id);
		long maxId = BitAddressUtil.toMaxAddress(id);
		boolean anyInterestingTileBehavior = false;
		
		for( Tile[] stack : tiles ) for( Tile t : stack ) {
			if( t.behavior != BoringestThingBehavior.instance ) {
				anyInterestingTileBehavior = true;
			}
			maxId = BitAddressUtil.maxAddressAI( maxId, t.id );
			minId = BitAddressUtil.minAddressAI( minId, t.id );
			nextAutoUpdateTime = Math.min(nextAutoUpdateTime, t.getNextAutoUpdateTime() );
		}
		for( DynamicThing t : dynamicThings ) {
			maxId = BitAddressUtil.maxAddressAI( maxId, t.id );
			minId = BitAddressUtil.minAddressAI( minId, t.id );
			nextAutoUpdateTime = Math.min(nextAutoUpdateTime, t.getNextAutoUpdateTime() );
		}
		
		this.minId = minId; this.maxId = maxId;
		this.anyTilesHaveInterestingBehavior = anyInterestingTileBehavior;
		this.nextAutoUpdateTime = nextAutoUpdateTime;
	}

	@Override public long getMinId() { return minId; }
	@Override public long getMaxId() { return maxId; }
	@Override public long getNextAutoUpdateTime() { return nextAutoUpdateTime; }
	
	protected Room updateComponents( SimNode rootNode, long timestamp, Message m, List<Message> messageDest ) {
		Tile[][] newTiles = tiles;
		if( anyTilesHaveInterestingBehavior ) {
			for( int i=0; i<tiles.length; ++i ) {
				Tile[] stack = tiles[i];
				List<Tile> newStack = null;
				for( int j=0; j<stack.length; ++j ) {
					Tile newTile = stack[j].update(rootNode, timestamp, m, messageDest);
					if( newTile != stack[j] ) {
						if( newTiles == tiles ) {
							newTiles = new Tile[tiles.length][];
							for( int k=0; k<tiles.length; ++k ) {
								newTiles[k] = tiles[k];
							}
						}
						if( newStack == null ) {
							newStack = new ArrayList<Tile>();
							for( int k=0; k<j; ++k ) {
								newStack.add(stack[k]);
							}
						}
					}
					if( newStack != null ) {
						newStack.add(newTile);
					}
				}
				if( newStack != null ) {
					newTiles[i] = newStack.toArray(new Tile[newStack.size()]);
				}
			}
		}
		
		List<DynamicThing> newDynamicThingList = null;
		for( int i=0; i<dynamicThings.length; ++i ) {
			DynamicThing newThing = dynamicThings[i].update( rootNode, timestamp, m, messageDest );
			if( newThing != dynamicThings[i] && newDynamicThingList == null ) {
				newDynamicThingList = new ArrayList<DynamicThing>();
				for( int j=0; j<i; ++j ) {
					newDynamicThingList.add(dynamicThings[j]);
				}
			}
			if( newDynamicThingList != null && newThing != null ) {
				newDynamicThingList.add(newThing);
			}
		}
		
		DynamicThing[] newDynamicThings =
			newDynamicThingList == null ? dynamicThings : newDynamicThingList.toArray(new DynamicThing[newDynamicThingList.size()]);
		
		return (newTiles != tiles || newDynamicThings != dynamicThings) ?
			new Room( id, width, height, depth, originX, originY, neighbors, backgroundLink, newTiles, newDynamicThings ) : this;
	}
	
	protected Room updateSelf( SimNode rootNode, long timestamp, Message m, List<Message> messageDest ) {
		// TODO: handle ADD_DYNAMIC_THING, etc
		return this;
	}
	
	@Override public Room update( SimNode rootNode, long timestamp, Message m, List<Message> messageDest ) {
		if( timestamp < nextAutoUpdateTime && !BitAddressUtil.rangesIntersect(m, minId, maxId) ) return this;
		
		Room newRoom = BitAddressUtil.rangeContains(m, id) ? updateSelf(rootNode, timestamp, m, messageDest) : this;
		return newRoom.updateComponents( rootNode, timestamp, m, messageDest );
	}
	@Override public <T> T get( long id, Class<T> expectedClass ) {
		if( id == this.id ) return expectedClass.cast(this);
		
		for( Tile[] stack : tiles ) for( Tile t : stack ) {
			if( t.id == id ) return expectedClass.cast(t);
		}
		
		for( DynamicThing d : dynamicThings ) {
			if( d.id == id ) return expectedClass.cast(d);
		}
		
		return null;
	}
	
	////
	
	public static class TileMapper {
		final StorageContext storageContext;
		ArrayList<ImageHandle> imagePalette = new ArrayList<ImageHandle>();
		HashMap<String,Integer> map = new HashMap<String,Integer>();
		
		public TileMapper( StorageContext ctx ) {
			this.storageContext = ctx;
		}		
		
		public SoftResourceHandle<ImageHandle[]> getImagePaletteHandle() {
			ImageHandle[] ih = imagePalette.toArray(new ImageHandle[imagePalette.size()]);
			try {
				return storageContext.resourceHandlePool.get(StorageUtil.storeSerialized(storageContext.blobRepository, ih));
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		}
		
		public byte imageUriToIndex( String imageUri ) {
			Integer i = map.get(imageUri);
			if( i != null ) return i.byteValue();
			if( imagePalette.size() >= 0xFF ) {
				throw new RuntimeException("Can't fit any more images into palette");
			}
			i = imagePalette.size();
			map.put(imageUri, i);
			imagePalette.add(storageContext.imageHandlePool.get(imageUri));
			return i.byteValue();
		}
	}
	
	protected static void _projectView(
		SimNode root, long roomId, Room r, int x, int y, int viz0,
		int bWidth, int bHeight, int bx, int by, int[] visibility, Room[] rooms, int[] cellIndexes,
		boolean adjustToNeighbor
	) {
		assert viz0 >= 0;
		
		int bIdx = by*bWidth+bx; // output Buffer index
		
		// Bail if out of outbut buffer bounds
		if( bx < 0 || by < 0 || bx >= bWidth || by >= bHeight ) return;
		
		// Ignore cells that have already been marked with >= viz0 visibility
		if( viz0 <= visibility[bIdx] ) return;
		
		// Load the room if needed
		if( r == null || r.id != roomId ) {
			r = root.get(roomId, Room.class);
		}
		
		// Figure tile x, y within room
		int tileX = x+r.originX, tileY = y+r.originY;
		
		// If out of bounds, figure out which neighbor that actually is
		if( tileX < 0 || tileY < 0 || tileX >= r.width || tileY >= r.height && adjustToNeighbor ) {
			for( Neighbor n : r.neighbors ) {
				if(
					x >= n.offsetX && y >= n.offsetY &&
					x < n.offsetX+n.width && y < n.offsetY+n.height
				) {
					_projectView(
						root, n.roomId, r, x-n.offsetX, y-n.offsetY, viz0,
						bWidth, bHeight, bx, by, visibility, rooms, cellIndexes, false
					);
					return; 
				}
			}
			return;
		}
		
		// Bail if out of room bounds
		if( tileX < 0 || tileY < 0 || tileX >= r.width || tileY >= r.height ) return;
		
		int cellIndex = tileX + tileY*r.height;
		rooms[bIdx] = r;
		visibility[bIdx] = viz0;
		cellIndexes[bIdx] = cellIndex;
		
		// If this is our last stop, then stop
		if( viz0 == 0 ) return;
		
		// If any tiles here are opaque, then stop
		for( int l=0; l<r.tiles.length; ++l ) {
			for( int z=0; z<r.depth; ++z ) {
				int tileIndex = cellIndex + r.width*r.height*z;
				Tile[] stack = r.tiles[tileIndex];
				for( Tile t : stack ) {
					if( t.isOpaque ) return;
				}
			}
		}
		
		// Otherwise recurse onto all the neighbors
		
		for( int i=0; i<4; ++i ) {
			int dx = (i&1) == 0 ? +1 : -1;
			int dy = (i&2) == 0 ? +1 : -1;
			_projectView(
				root, roomId, r, x+dx, y+dy, viz0,
				bWidth, bHeight, bx+dx, by+dy, visibility, rooms, cellIndexes, true
			);
		}
	}
	
	protected static void projectView(
		SimNode root, long roomId, int x, int y, int viz0,
		int bWidth, int bHeight, int bx, int by, int[] visibility, Room[] rooms, int[] cellIndexes
	) {
		_projectView(
			root, roomId, null, x, y, viz0,
			bWidth, bHeight, bx, by, visibility, rooms, cellIndexes,
			false
		);
	}
	
	public static VizState toVizState( TileMapper tileMapper, SimNode root, long roomId, float eyeX, float eyeY, float eyeZ, int vizDist ) {
		int bSize = vizDist*2+1;
		Room[] rooms = new Room[bSize*bSize];
		int[] cellIndexes = new int[bSize*bSize];
		int[] visibility = new int[bSize*bSize];
		
		Room r = root.get(roomId, Room.class);
		
		projectView(
			root, roomId, (int)eyeX, (int)eyeY, vizDist,
			bSize, bSize, vizDist, vizDist, visibility, rooms, cellIndexes
		);
		
		int maxRoomDepth = 0;
		
		// Assume all rooms start at the same Z
		for( int i=0; i<rooms.length; ++i ) {
			Room rx = rooms[i];
			if( rx == null ) continue;
			if( rx.depth > maxRoomDepth ) maxRoomDepth = rx.depth;
		}
		
		// TODO: VizState isn't equipped to handle multiple tiles per cell!!!
		byte[][] tileLayers = new byte[maxRoomDepth][bSize*bSize];
		for( int i=0; i<rooms.length; ++i ) {
			Room rx = rooms[i];
			if( rx == null ) continue;
			for( int z=0; z<rx.depth; ++z ) {
				Tile[] stack = r.tiles[r.width*r.height*z + cellIndexes[i]];
				if( stack.length >= 1 ) {
					// ... so just getting the first tile in the stack
					tileLayers[z][i] = tileMapper.imageUriToIndex(stack[0].imageUri);
				}
			}
		}
		
		boolean[] cornerVisibility = new boolean[(bSize+1)*(bSize+1)];
		for( int y=1; y<bSize; ++y ) for( int x=1; x<bSize; ++x ) {
			cornerVisibility[y*(bSize+1)+x] = true ||
				(visibility[(y-1)*bSize+(x-1)] > 0) &&
				(visibility[(y-1)*bSize+(x  )] > 0) &&
				(visibility[(y  )*bSize+(x-1)] > 0) &&
				(visibility[(y  )*bSize+(x  )] > 0);
		}
		
		// TODO: collect sprites somehow
		
		Sprite[] sprites = new Sprite[0];

		return new VizState(
			bSize, vizDist+0.5f, vizDist+0.5f, 0, 0,
			new BackgroundLink[1], new byte[bSize*bSize],
			tileMapper.getImagePaletteHandle(), tileLayers,
			cornerVisibility, sprites
		);
	}
}
