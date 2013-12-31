package togos.networkrts.experimental.game18.sim;

import java.util.ArrayList;
import java.util.List;

import togos.networkrts.experimental.qt2drender.VizState.BackgroundLink;

public class Room implements SimNode
{
	static class Neighbor {
		final long id;
		final int offsetX, offsetY, offsetZ;
		
		public Neighbor( long id, int offsetX, int offsetY, int offsetZ ) {
			this.id = id;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
		}
	}
	
	interface ThingBehavior<Thing> {
		public long getNextAutoUpdateTime( Thing t );
		public Thing update( Thing t, SimNode rootNode, long timestamp, Message m, List<Message> messageQueue );
	}
	
	static class BoringestThingBehavior<Thing> implements ThingBehavior<Thing> {
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
	
	static class Tile implements SimNode {
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
		
		final Tile[] single = new Tile[] { this };
		
		public Tile( long id, String imageUri, boolean isSolid, boolean isOpaque, ThingBehavior<Tile> behavior ) {
			this.id = id;
			this.imageUri = imageUri;
			this.isSolid = isSolid;
			this.isOpaque = isOpaque;
			this.behavior = behavior;
		}

		@Override public long getMinId() {
			return id == Util.NO_ID ? Util.MAX_ID : id;
		}
		@Override public long getMaxId() {
			return id == Util.NO_ID ? Util.MIN_ID : id;
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
	
	static class DynamicThing implements SimNode {
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
			return id == Util.NO_ID ? Util.MAX_ID : id;
		}
		@Override public long getMaxId() {
			return id == Util.NO_ID ? Util.MIN_ID : id;
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
	
	final long id;
	final int width, height, depth;
	final int originX, originY;
	final BackgroundLink backgroundLink;
	final Tile[][] tiles;
	final DynamicThing[] dynamicThings;
	
	protected final long minId, maxId;
	protected final boolean anyTilesHaveInterestingBehavior;
	protected final long nextAutoUpdateTime;
	
	public Room(
		long id, int width, int height, int depth, int originX, int originY,
		BackgroundLink backgroundLink, Tile[][] tiles, DynamicThing[] dynamicThings
	) {
		this.id = id;
		this.width = width; this.height = height; this.depth = depth;
		this.originX = originX; this.originY = originY;
		this.backgroundLink = backgroundLink;
		this.tiles = tiles; this.dynamicThings = dynamicThings;
		
		long nextAutoUpdateTime = Long.MAX_VALUE;
		long minId = Util.toMinId(id);
		long maxId = Util.toMaxId(id);
		boolean anyInterestingTileBehavior = false;
		
		for( Tile[] stack : tiles ) for( Tile t : stack ) {
			if( t.behavior != BoringestThingBehavior.instance ) {
				anyInterestingTileBehavior = true;
			}
			maxId = Util.maxId( maxId, t.id );
			minId = Util.minId( minId, t.id );
			nextAutoUpdateTime = Math.min(nextAutoUpdateTime, t.getNextAutoUpdateTime() );
		}
		for( DynamicThing t : dynamicThings ) {
			maxId = Util.maxId( maxId, t.id );
			minId = Util.minId( minId, t.id );
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
			new Room( id, width, height, depth, originX, originY, backgroundLink, tiles, dynamicThings ) : this;
	}
	
	protected Room updateSelf( SimNode rootNode, long timestamp, Message m, List<Message> messageDest ) {
		// TODO: handle ADD_DYNAMIC_THING, etc
		return this;
	}
	
	@Override public Room update( SimNode rootNode, long timestamp, Message m, List<Message> messageDest ) {
		if( timestamp < nextAutoUpdateTime && !Util.rangesIntersect(m.minId, m.maxId, minId, maxId) ) return this;
		
		Room newRoom = Util.rangeContains(m.minId, m.maxId, id) ? updateSelf(rootNode, timestamp, m, messageDest) : this;
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
}
