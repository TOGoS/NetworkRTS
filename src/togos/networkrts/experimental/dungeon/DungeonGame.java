package togos.networkrts.experimental.dungeon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import togos.networkrts.experimental.dungeon.Room.Neighbor;
import togos.networkrts.experimental.dungeon.net.EthernetPort;
import togos.networkrts.experimental.dungeon.net.ObjectEthernetFrame;
import togos.networkrts.experimental.gensim.AutoEventUpdatable;

public class DungeonGame
{
	static interface MessageReceiver {
		public void messageReceived( long time, Object message );
	}
	static interface UpdateListener {
		public void updated( long time );
	}
	
	static class DGTimer implements Comparable<DGTimer> {
		public final long time;
		public final MessageReceiver target;
		public final Object payload;
		
		public DGTimer( long time, MessageReceiver target, Object payload ) {
			this.time = time;
			this.target = target;
			this.payload = payload;
		}
		
		@Override public int compareTo(DGTimer o) {
			return time < o.time ? -1 : time > o.time ? 1 : 0;
		}
	}
	
	/**
	 * Tracks most recent projection,
	 * whether that projection is still valid,
	 * and handles adding and removing itself from rooms' watcher lists.
	 */
	static class VisibilityCache implements RoomWatcher, UpdateListener
	{
		protected CellCursor position;
		public final InternalUpdater updater;
		public boolean valid = false, updated = false;
		public final Projection projection;
		protected HashSet<UpdateListener> updateListeners = new HashSet<UpdateListener>();
		
		public VisibilityCache( int w, int h, int d, InternalUpdater updater ) {
			this.projection = new Projection( w, h, d );
			this.updater = updater;
		}
		
		public void clear() {
			for( Room r : projection.roomsIncluded ) {
				r.watchers.remove(this);
			}
			projection.clear();
			valid = false;
		}
		
		public void rescan() {
			clear();
			if( position != null ) {
				projection.projectFrom(position);
				for( Room r : projection.roomsIncluded ) {
					r.watchers.add(this);
				}
			}
			valid = true;
			updated = true;
		}
		
		@Override public void roomUpdated(Room r) {
			invalidate();
		}
		
		public void addUpdateListener( UpdateListener l ) {
			this.updateListeners.add(l);
		}
		
		protected void invalidate() {
			this.valid = false;
			updater.addPostUpdateListener(this);
		}
		
		public void setPosition( CellCursor pos ) {
			this.position = pos;
			invalidate();
		}
		
		
		
		/**
		 * Should only be triggered during post-event processing in response to
		 * invalidate();
		 */
		public void updated( long time ) {
			if( !valid ) rescan();
			for( UpdateListener l : updateListeners ) l.updated( time );
		}
	}
	
	static class WalkingCharacter extends CellCursor implements MessageReceiver
	{
		protected InternalUpdater updater;
		private VisibilityCache visibilityCache;
		public int facingX = 0, facingY = 0;
		public int walkingX = 0, walkingY = 0;
		public long walkReadyTime = 0;
		public long walkStepInterval = 100; // Interval between steps
		public long blockDelay = 10; // Delay after being blocked
		public Block block;
		public long clientEthernetAddress = 0;
		public long uplinkInterfaceAddress = 0;
		public EthernetPort uplink;
		
		public boolean watching; // TODO: Need to store a set of visible rooms, and probably store callbacks on said rooms
		
		public WalkingCharacter( Block block, InternalUpdater updater ) {
			this.block = block;
			this.updater = updater;
		}
		
		public void set( Room r, float x, float y, float z ) {
			super.set(r,x,y,z);
			if( visibilityCache != null ) visibilityCache.setPosition(this);
		}
		
		public void putAt( Room r, float x, float y, float z) {
			set(r, x, y, z);
			addBlock(block);
		}
		
		public void setVisibilityCache(VisibilityCache vc) {
			if( this.visibilityCache != null ) {
				this.visibilityCache.clear();
			}
			this.visibilityCache = vc;
			vc.setPosition(this);
		}
		
		public void startWalking(int x, int y, int z) {
			this.facingX = x;
			this.facingY = y;
			this.walkingX = x;
			this.walkingY = y;
		}
		
		public void stopWalking() {
			this.walkingX = 0;
			this.walkingY = 0;
		}
		
		@Override public void messageReceived(long time, Object message) {
			if( message instanceof WalkCommand ) {
				WalkCommand wc = (WalkCommand)message;
				startWalking( wc.walkX, wc.walkY, 0 );
			}
		}
	}
	
	static class WalkCommand {
		public int walkX, walkY;
	}
	
	interface InternalUpdater {
		public long getCurrentTime();
		public void addTimer( long timestamp, MessageReceiver dest, Object payload );
		public void addPostUpdateListener( UpdateListener ent );
	}
	
	static class Simulator implements AutoEventUpdatable<ObjectEthernetFrame<?>> {
		final PriorityQueue<DGTimer> timerQueue = new PriorityQueue<DGTimer>();
		/**
		 * After all message timers for a given time have been run,
		 * all postUpdateListeners will be updated.
		 * 
		 * These may induce a new set of immediate message timers,
		 * but this should be avoided, esp. where it may cause cascading updates.
		 * 
		 * They may NOT add new post-update listeners
		 */
		final HashSet<UpdateListener> postUpdateListeners = new HashSet<UpdateListener>();
		
		protected final InternalUpdater internalUpdater = new InternalUpdater() {
			@Override public long getCurrentTime() {
				return currentTime;
			}
			@Override public void addTimer(long time, MessageReceiver target, Object payload) {
				timerQueue.add(new DGTimer(time, target, payload));
			}
			@Override public void addPostUpdateListener(UpdateListener ent) {
				postUpdateListeners.add(ent);
			}
		};
		
		public InternalUpdater getInternalUpdater() { return internalUpdater; }
		
		public WalkingCharacter commandee = null;
		List<WalkingCharacter> characters = new ArrayList<WalkingCharacter>();
		long currentTime = 0;
		
		final CellCursor tempCursor = new CellCursor();
		
		/**
		 * When called during handling of an event or time step,
		 * returns the time of the event being processed. 
		 */
		public long getCurrentTime() {
			return currentTime;
		}
		
		protected boolean attemptMove( WalkingCharacter c, int dx, int dy, int dz ) {
			Room oldRoom = c.room;
			tempCursor.set(c);
			tempCursor.move( dx, dy, dz );
			boolean blocked = false;
			for( Block b : tempCursor.getAStack() ) {
				if( b.blocking ) blocked = true;
			}
			if( !blocked ) {
				c.removeBlock(c.block);
				c.set(tempCursor);
				c.addBlock(c.block);
				oldRoom.updated();
				if( c.room != oldRoom ) c.room.updated();
				return true;
			} else {
				return false;
			}
		}
		
		public synchronized void doCharacterPhysics( WalkingCharacter c ) {
			if( c.walkReadyTime > currentTime ) return;
			
			boolean movedX = false, movedY = false;
			boolean blockedX = false;
			if( c.walkingX != 0 ) {
				movedX = attemptMove( c, c.walkingX, 0, 0 );
				blockedX = !movedX;
			}
			if( c.walkingY != 0 ) {
				movedY = attemptMove( c, 0, c.walkingY, 0 );
			}
			if( blockedX && movedY ) {
				// Then try moving X-wise again!
				movedX = attemptMove( c, c.walkingX, 0, 0 );
			}
			if( movedX || movedY ) {
				c.walkReadyTime = currentTime + c.walkStepInterval;
			} else {
				c.walkReadyTime = currentTime + c.blockDelay;
			}
			
			// Force an update at that time:
			internalUpdater.addTimer(c.walkReadyTime, null, null);
		}
		
		@Override public long getNextAutomaticUpdateTime() {
			long nextAutoUpdateTime = TIME_INFINITY;
			DGTimer firstTimer;
			if( (firstTimer = timerQueue.peek()) != null ) {
				nextAutoUpdateTime = Math.min(nextAutoUpdateTime, firstTimer.time);
			}
			if( nextAutoUpdateTime <= currentTime ) {
				nextAutoUpdateTime = currentTime;
			}
			return nextAutoUpdateTime;
		}
		
		protected void runPostUpdateListeners() {
			for( UpdateListener e : postUpdateListeners ) e.updated(currentTime);
			postUpdateListeners.clear();
		}
		
		protected void timersRan() {
			for( WalkingCharacter c : characters ) {
				doCharacterPhysics( c );
			}
			runPostUpdateListeners();
		}
			
		
		protected void fastForward( long endTime ) {
			DGTimer t;
			while( (t = timerQueue.peek()) != null && t.time <= endTime ) {
				t = timerQueue.remove();
				if( t.time != currentTime ) timersRan();
				currentTime = t.time;
				if( t.target != null ) t.target.messageReceived(currentTime, t.payload);
			}
			if( endTime != currentTime ) timersRan();
			currentTime = endTime;
		}
		
		// TODO: replace this with a proper switch
		EthernetPort ioPort = new EthernetPort() {
			@Override public void put(long time, ObjectEthernetFrame f) {
				for( WalkingCharacter character : characters ) {
					if( f != null ) {
						if( f.destAddress == character.uplinkInterfaceAddress ) {
							character.messageReceived(time, f.payload);
						}
					}
				}
			}
		};
		
		@Override
		public Simulator update( long time, ObjectEthernetFrame<?> f ) throws Exception {
			fastForward( time ); // Fast forward to the proper time
			ioPort.put(time, f); // Process the incoming event
			fastForward( time ); // Process any remaining immediate events
			timersRan();
			return this;
		}
	}
	
	public static Simulator initSim( long initialTime ) {
		Room r0 = new Room(64, 64, 4, Block.EMPTY_STACK);
		for( int y=r0.getHeight()-1; y>=0; --y )
		for( int x=r0.getWidth()-1; x>=0; --x ) {
			r0.blockField.setStack(x, y, 0, Block.GRASS.stack);
		}
		
		CellCursor c = new CellCursor();
		CellCursor c1 = new CellCursor();
		Random rand = new Random();
		for( int i=0; i<100; ++i ) {
			c.set(r0, rand.nextInt(64), rand.nextInt(64), 1);
			int r = rand.nextInt(3);
			for( int dy=0; dy<=1+r; ++dy )
			for( int dx=0; dx<=1+r; ++dx ) {
				c1.set(c);
				c1.move(dx, dy, 0);
				c1.setStack( Block.FOLIAGE.stack );
			}
		}
		for( int i=0; i<20; ++i ) {
			c.set(r0, rand.nextInt(64), rand.nextInt(64), 1);
			for( int dy=-1; dy<=1; ++dy )
			for( int dx=-1; dx<=1; ++dx ) {
				c1.set(c);
				c1.move(dx, dy, 0);
				c1.setStack( Block.WALL.stack );
			}
		}
		
		r0.neighbors.add(new Neighbor(r0,  64,  32, 0));
		r0.neighbors.add(new Neighbor(r0, -64, -32, 0));
		r0.neighbors.add(new Neighbor(r0,  64, -32, 0));
		r0.neighbors.add(new Neighbor(r0, -64,  32, 0));
		r0.neighbors.add(new Neighbor(r0,   0, -64, 0));
		r0.neighbors.add(new Neighbor(r0,   0,  64, 0));
		
		final Simulator sim = new Simulator();
		
		final WalkingCharacter player = new WalkingCharacter( Block.PLAYER, sim.getInternalUpdater() );
		player.walkReadyTime = initialTime;
		player.putAt( r0, 2.51f, 2.51f, 1.51f );
		sim.commandee = player;
		sim.characters.add(player);
		
		final WalkingCharacter bot = new WalkingCharacter( Block.BOT, sim.getInternalUpdater() );
		bot.putAt( r0, 3.51f, 3.51f, 1.51f);
		bot.startWalking( 1, 1, 0);
		sim.characters.add(bot);
		
		final WalkingCharacter bot2 = new WalkingCharacter( Block.BOT, sim.getInternalUpdater() );
		bot2.putAt( r0, 4.51f, 3.51f, 1.51f);
		bot2.startWalking( 1, 1, 0);
		sim.characters.add(bot2);
		
		final WalkingCharacter bot3 = new WalkingCharacter( Block.BOT, sim.getInternalUpdater() );
		bot3.putAt( r0, 1.51f, 3.51f, 1.51f);
		bot3.startWalking( 1, 1, 0);
		sim.characters.add(bot3);
		
		sim.internalUpdater.addTimer(initialTime, null, null);

		return sim;
	}
}
