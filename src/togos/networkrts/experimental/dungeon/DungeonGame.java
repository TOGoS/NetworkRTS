package togos.networkrts.experimental.dungeon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import togos.networkrts.experimental.dungeon.Room.Neighbor;
import togos.networkrts.experimental.dungeon.net.AbstractConnector;
import togos.networkrts.experimental.dungeon.net.ConnectionError;
import togos.networkrts.experimental.dungeon.net.Connector;
import togos.networkrts.experimental.dungeon.net.ConnectorTypes;
import togos.networkrts.experimental.dungeon.net.Connectors;
import togos.networkrts.experimental.dungeon.net.EthernetSwitch;
import togos.networkrts.experimental.dungeon.net.ObjectEthernetFrame;
import togos.networkrts.experimental.dungeon.net.PatchCable;
import togos.networkrts.experimental.gensim.AutoEventUpdatable;

public class DungeonGame
{
	static class DGTimer<Payload> implements Comparable<DGTimer<?>> {
		public final long time;
		public final MessageReceiver<? super Payload> target;
		public final Payload payload;
		
		public DGTimer( long time, MessageReceiver<? super Payload> target, Payload payload ) {
			this.time = time;
			this.target = target;
			this.payload = payload;
		}
		
		@Override public int compareTo(DGTimer<?> o) {
			return time < o.time ? -1 : time > o.time ? 1 : 0;
		}
	}
	
	static interface DGPhysicalObject {
		
	};
	
	/**
	 * Tracks most recent projection,
	 * whether that projection is still valid,
	 * and handles adding and removing itself from rooms' watcher lists.
	 */
	static class VisibilityCache implements RoomWatcher, UpdateListener
	{
		private final CellCursor position = new CellCursor();
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
		
		public void removeUpdateListener( UpdateListener l ) {
			this.updateListeners.remove(l);
		}
		public void addUpdateListener( UpdateListener l ) {
			this.updateListeners.add(l);
		}
		
		protected void invalidate() {
			this.valid = false;
			updater.addPostUpdateListener(this);
		}
		
		public void setPosition( CellCursor pos ) {
			this.position.set(pos);
			invalidate();
		}
		
		/**
		 * Should only be triggered during post-event processing in response to
		 * invalidate();
		 */
		public void updated() {
			if( !valid ) rescan();
			for( UpdateListener l : updateListeners ) l.updated();
		}
	}
	
	static class WalkingCharacter implements DGPhysicalObject, MessageReceiver<Object>
	{
		protected final InternalUpdater updater;
		protected VisibilityCache visibilityCache;
		public int facingX = 0, facingY = 0;
		public int walkingX = 0, walkingY = 0;
		public long walkReadyTime = 0;
		public long walkStepInterval = 100; // Interval between steps
		public long blockDelay = 10; // Delay after being blocked
		public Block block;
		private final CellCursor position = new CellCursor();
		
		public WalkingCharacter( Block block, InternalUpdater updater ) {
			this.block = block;
			this.updater = updater;
		}
		
		protected void preMove( boolean roomChanged ) {
			position.removeBlock(block);
			// If room has not changed, we'll only call room.updated() once, in postMove
			if( roomChanged && position.room != null ) position.room.updated();
		}
		protected void postMove( boolean roomChanged ) {
			position.addBlock(block);
			if( visibilityCache != null ) visibilityCache.setPosition(this.position);
			if( position.room != null ) position.room.updated();
		}
		
		public void setPosition( Room r, float x, float y, float z ) {
			boolean roomChanged = r != this.position.room;
			preMove( roomChanged );
			position.set(r, x, y, z);
			postMove( roomChanged );
		}
		
		public void setPosition( CellCursor pos ) {
			setPosition(pos.room, pos.x, pos.y, pos.z);
		}
		
		public void getPosition(CellCursor dest) {
			dest.set(position);
		}
		
		public void putAt( Room r, float x, float y, float z) {
			setPosition(r, x, y, z);
			position.addBlock(block);
		}
		
		public void setVisibilityCache(VisibilityCache vc) {
			if( this.visibilityCache != null ) {
				this.visibilityCache.clear();
			}
			this.visibilityCache = vc;
			vc.setPosition(this.position);
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
		
		@Override public void messageReceived(Object message) {
			if( message instanceof WalkCommand ) {
				WalkCommand wc = (WalkCommand)message;
				startWalking( wc.walkX, wc.walkY, 0 );
			}
		}
	}
	
	static class AvatarianTranceiver implements DGPhysicalObject {
		protected final InternalUpdater updater;
		protected final long transmissionDelay;
		protected AvatarianTranceiver other;
		
		private AvatarianTranceiver( long transmissionDelay, InternalUpdater updater ) {
			this.updater = updater;
			this.transmissionDelay = transmissionDelay;
		}
		
		protected MessageReceiver<ObjectEthernetFrame<?>> transmissionReceiver = new MessageReceiver<ObjectEthernetFrame<?>>() {
			@Override public void messageReceived(ObjectEthernetFrame<?> message) {
				port.sendMessage(message);
			}
		};
		
		public final AbstractConnector<ObjectEthernetFrame<?>> port = new AbstractConnector<ObjectEthernetFrame<?>>(ConnectorTypes.rj45.female, ObjectEthernetFrame.GENERIC_CLASS) {
			@Override public boolean isLocked() { return false; }
			@Override public void messageReceived(ObjectEthernetFrame<?> message) {
				updater.addTimer(updater.getCurrentTime()+transmissionDelay, other.transmissionReceiver, message);
			}
		};
		
		public static AvatarianTranceiver[] makePair(long transmissionDelay, InternalUpdater updater) {
			AvatarianTranceiver t1 = new AvatarianTranceiver(transmissionDelay, updater);
			AvatarianTranceiver t2 = new AvatarianTranceiver(transmissionDelay, updater);
			t1.other = t2;
			t2.other = t1;
			return new AvatarianTranceiver[]{ t1, t2 };
		}
	}
	
	static class Avatar extends WalkingCharacter
	{
		// TODO: Use more generic physical container system
		AvatarianTranceiver atc;
		EthernetSwitch internalSwitch = new EthernetSwitch(6, 0, updater);
		final HashSet<DGPhysicalObject> internalItems = new HashSet<DGPhysicalObject>();
		public Connector<ObjectEthernetFrame<?>> headPort;
		public long avatarEthernetAddress = 0x1234567;
		public long clientEthernetAddress;
		
		public final AbstractConnector<ObjectEthernetFrame<?>> controllerPort = new AbstractConnector<ObjectEthernetFrame<?>>(ConnectorTypes.rj45.female, ObjectEthernetFrame.GENERIC_CLASS) {
			@Override public boolean isLocked() { return false; }
			@Override public void messageReceived(ObjectEthernetFrame<?> message) {
				if( avatarEthernetAddress == message.destAddress ) {
					Avatar.this.messageReceived(message.payload);
				}
			}
		};
		
		public Avatar( Block block, AvatarianTranceiver atc, InternalUpdater updater ) {
			super(block, updater);
			PatchCable<ObjectEthernetFrame<?>> headPortCable = new PatchCable<ObjectEthernetFrame<?>>(ConnectorTypes.rj45.male, ConnectorTypes.rj45.female, ObjectEthernetFrame.GENERIC_CLASS, 0, updater);
			PatchCable<ObjectEthernetFrame<?>> uplinkCable = new PatchCable<ObjectEthernetFrame<?>>(ConnectorTypes.rj45.male, ConnectorTypes.rj45.male, ObjectEthernetFrame.GENERIC_CLASS, 0, updater);
			PatchCable<ObjectEthernetFrame<?>> controllerCable = new PatchCable<ObjectEthernetFrame<?>>(ConnectorTypes.rj45.male, ConnectorTypes.rj45.male, ObjectEthernetFrame.GENERIC_CLASS, 0, updater);
			this.atc = atc;
			this.headPort = headPortCable.right;
			try {
				Connectors.connect(internalSwitch.getPort(2), headPortCable.left);
				
				Connectors.connect(internalSwitch.getPort(0), uplinkCable.right);
				Connectors.connect(atc.port, uplinkCable.left);
				
				Connectors.connect(internalSwitch.getPort(1), controllerCable.right);
				Connectors.connect(controllerPort, controllerCable.left);
			} catch( ConnectionError e ) {
				System.err.println("Failed to connect head internals: "+e.getMessage());
			}
		}
		
		public <Payload> void sendFrame( long destAddress, Payload payload ) {
			if( destAddress != 0 ) {
				controllerPort.sendMessage(new ObjectEthernetFrame<Payload>(avatarEthernetAddress, destAddress, payload));
			}
		}
		
		protected UpdateListener vcUpdateListener = new UpdateListener() {
			public void updated() {
				sendFrame(clientEthernetAddress, visibilityCache.projection.clone());
			}
		};
		
		@Override public void setVisibilityCache(VisibilityCache vc) {
			VisibilityCache oldVc = this.visibilityCache;
			if( oldVc != null ) oldVc.removeUpdateListener(vcUpdateListener);
			
			super.setVisibilityCache(vc);
			if( vc != null ) vc.addUpdateListener(vcUpdateListener);
			
			vcUpdateListener.updated();
		}
	}
	
	static class WalkCommand {
		public int walkX, walkY;
	}
	
	public interface InternalUpdater {
		public long getCurrentTime();
		public <Payload> void addTimer( long timestamp, MessageReceiver<? super Payload> dest, Payload payload );
		public void addPostUpdateListener( UpdateListener ent );
	}
	
	static class Simulator implements AutoEventUpdatable<DGTimer<?>> {
		final PriorityQueue<DGTimer<?>> timerQueue = new PriorityQueue<DGTimer<?>>();
		/**
		 * After all message timers for a given time have been run,
		 * all postUpdateListeners will be updated.
		 * 
		 * These may induce a new set of immediate message timers,
		 * but this should be avoided, esp. where it may cause cascading updates.
		 * 
		 * These may NOT add new post-update listeners
		 */
		final HashSet<UpdateListener> postUpdateListeners = new HashSet<UpdateListener>();
		
		protected final InternalUpdater internalUpdater = new InternalUpdater() {
			@Override public long getCurrentTime() {
				return currentTime;
			}
			@Override public <Payload> void addTimer(long time, MessageReceiver<? super Payload> target, Payload payload) {
				timerQueue.add(new DGTimer<Payload>(time, target, payload));
			}
			@Override public void addPostUpdateListener(UpdateListener ent) {
				postUpdateListeners.add(ent);
			}
		};
		
		public InternalUpdater getInternalUpdater() { return internalUpdater; }
		
		public Avatar commandee = null;
		public Connector<ObjectEthernetFrame<?>> playerRemoteTranceiverPort;
		
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
			c.getPosition(tempCursor);
			tempCursor.changePosition( dx, dy, dz );
			boolean blocked = false;
			for( Block b : tempCursor.getAStack() ) {
				if( b.blocking ) blocked = true;
			}
			if( !blocked ) {
				c.setPosition(tempCursor);
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
			DGTimer<?> firstTimer;
			if( (firstTimer = timerQueue.peek()) != null ) {
				nextAutoUpdateTime = Math.min(nextAutoUpdateTime, firstTimer.time);
			}
			if( nextAutoUpdateTime <= currentTime ) {
				nextAutoUpdateTime = currentTime;
			}
			return nextAutoUpdateTime;
		}
		
		protected void runPostUpdateListeners() {
			for( UpdateListener e : postUpdateListeners ) e.updated();
			postUpdateListeners.clear();
		}
		
		protected void doPostMessageUpdates() {
			for( WalkingCharacter c : characters ) {
				doCharacterPhysics( c );
			}
			runPostUpdateListeners();
		}
		
		protected void timePassed(long newTime) {
			if( currentTime == newTime ) {
				// No, it didn't!
				return;
			}
			
			currentTime = newTime;
			doPostMessageUpdates();
		}
		
		protected <Payload> void deliver( DGTimer<Payload> t ) {
			if( t.target == null ) return;
			t.target.messageReceived(t.payload);
		}
		
		protected void fastForward( long endTime ) {
			DGTimer<?> t;
			while( (t = timerQueue.peek()) != null && t.time <= endTime ) {
				t = timerQueue.remove();
				timePassed(Math.max(t.time, currentTime));
				deliver(t);
			}
			currentTime = endTime;
		}
		
		@Override
		public Simulator update( long time, DGTimer<?> t ) throws Exception {
			fastForward( time );
			if( t != null ) {
				timerQueue.add(t);
				fastForward( time );
			}
			doPostMessageUpdates();
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
				c1.changePosition(dx, dy, 0);
				c1.setStack( Block.FOLIAGE.stack );
			}
		}
		for( int i=0; i<20; ++i ) {
			c.set(r0, rand.nextInt(64), rand.nextInt(64), 1);
			for( int dy=-1; dy<=1; ++dy )
			for( int dx=-1; dx<=1; ++dx ) {
				c1.set(c);
				c1.changePosition(dx, dy, 0);
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
		
		final AvatarianTranceiver[] playerTranceivers = AvatarianTranceiver.makePair(0, sim.internalUpdater);
		
		sim.playerRemoteTranceiverPort = playerTranceivers[1].port;
		
		final Avatar player = new Avatar( Block.PLAYER, playerTranceivers[0], sim.getInternalUpdater() );
		player.walkReadyTime = initialTime;
		player.putAt( r0, 2.51f, 2.51f, 1.51f );
		sim.commandee = player;
		sim.characters.add(player);
		
		final WalkingCharacter bot = new WalkingCharacter( Block.BOT, sim.getInternalUpdater() );
		bot.walkStepInterval = 75;
		bot.putAt( r0, 3.51f, 3.51f, 1.51f);
		bot.startWalking( 1, 1, 0);
		sim.characters.add(bot);
		
		final WalkingCharacter bot2 = new WalkingCharacter( Block.BOT, sim.getInternalUpdater() );
		bot2.walkStepInterval = 125;
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
