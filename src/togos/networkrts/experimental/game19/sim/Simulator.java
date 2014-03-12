package togos.networkrts.experimental.game19.sim;

import java.util.ArrayList;
import java.util.Collection;

import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.EntityRanges;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.experimental.gameengine1.index.EntityUpdater;

public class Simulator
{
	// This is terribly over-engineered
	// It's meant as a somewhat thread-safe (if used as intended) 
	// way to collect both incoming messages and messages
	// generated within the simulation to be later (after frozen)
	// presented as a MessageSet
	static class ArrayMessageSet extends ArrayList<Message> implements MessageSet, UpdateContext
	{
		protected static final ArrayMessageSet EMPTY = new ArrayMessageSet().freeze();
		
		private static final long serialVersionUID = 1L;
		
		public ArrayMessageSet() { super(); }
		public ArrayMessageSet(Collection<Message> m) { super(m); }
		
		@Override public MessageSet subsetApplicableTo( double minX, double minY, double maxX, double maxY, long minBitAddress, long maxBitAddress ) {
			// TODO?
			return this;
		}
		
		boolean frozen = false;
		public synchronized ArrayMessageSet freeze() {
			if( size() == 0 && EMPTY != null ) return EMPTY;
			frozen = true;
			return this;
		}
		public ArrayMessageSet with( Message m ) {
			synchronized(this) {
				if( !frozen ) {
					add(m);
					return this;
				}
			}
			ArrayMessageSet ms = new ArrayMessageSet(this);
			ms.add(m);
			return ms;
		}
		
		public ArrayMessageSet cleared() {
			synchronized( this ) {
				if( !frozen ) {
					clear();
					return this;
				}
			}
			return new ArrayMessageSet();
		}
		
		// Under 'correct' use, this does not need to be synchronized
		@Override public synchronized void sendMessage( Message m ) {
			assert !frozen;
			add(m);
		}
	}
	
	protected World world;
	protected ArrayMessageSet incomingMessages = new ArrayMessageSet();
	
	public Simulator(World world) {
		this.world = world;
	}
	
	public World getWorld() {
		return world;
	}
	
	protected NonTile updateNonTile( NonTile nt, long time, World w, MessageSet incomingMessages, NonTileUpdateContext updateContext ) {
		nt = nt.withUpdatedPosition(time);
		nt = nt.behavior.update( nt, time, w, incomingMessages, updateContext );
		return nt;
	}
	
	static class NNTLNonTileUpdateContext implements NonTileUpdateContext {
		protected final Collection<NonTile> nonTileList;
		protected final UpdateContext updateContext;
		
		private NNTLNonTileUpdateContext( UpdateContext updateContext, Collection<NonTile> nonTileList ) {
			this.updateContext = updateContext;
			this.nonTileList = nonTileList;
		}
		
		public static NNTLNonTileUpdateContext get( NNTLNonTileUpdateContext oldInstance, UpdateContext updateContext, Collection<NonTile> nonTileList ) {
			return oldInstance == null || oldInstance.nonTileList == nonTileList ?
				oldInstance : new NNTLNonTileUpdateContext(updateContext, nonTileList);
		}
		
		@Override public void sendMessage( Message m ) { updateContext.sendMessage(m); }
		@Override public void addNonTile( NonTile nt ) { nonTileList.add(nt); }
	}
	
	protected EntitySpatialTreeIndex<NonTile> updateNonTiles( final World w, final long time, final MessageSet incomingMessages, final UpdateContext updateContext ) {
		return world.nonTiles.updateEntities(EntityRanges.BOUNDLESS, new EntityUpdater<NonTile>() {
			// TODO: this is very unoptimized
			// it doesn't take advantage of the tree structure at all
			// for determining what messages need to be delivered, etc
			// which could be a problem if there are lots of entities * lots of messages
			
			NNTLNonTileUpdateContext nntlntuc;
			
			@Override public NonTile update(NonTile nt, Collection<NonTile> generatedNonTiles) {
				return updateNonTile(nt, time, w, incomingMessages,
					NNTLNonTileUpdateContext.get(nntlntuc, updateContext, generatedNonTiles));
			}
		});
	}
	
	protected void update( long time, MessageSet incomingMessages ) {
		do {
			ArrayMessageSet newMessages = new ArrayMessageSet();
			int rstSize = 1<<world.rstSizePower;
			world = new World(
				world.rst.update( -rstSize/2, -rstSize/2, world.rstSizePower, time, incomingMessages, newMessages ),
				world.rstSizePower,
				updateNonTiles(world, time, incomingMessages, newMessages)
			);
			incomingMessages = newMessages;
		} while( incomingMessages.size() > 0 );
	}
	
	public void update( long time) {
		// TODO: This is not thread-safe at all!
		// In case someone's thinking of adding incoming messages
		// e.g. as they are received from a socket.
		// I expect this to be completely rewritten, anyway.
		update( time, takeIncomingMessages() );
	}
	
	protected synchronized MessageSet takeIncomingMessages() {
		try {
			return incomingMessages.freeze();
		} finally {
			incomingMessages = incomingMessages.cleared();
		}
	}
	
	public synchronized void enqueueMessage( Message m ) {
		incomingMessages = incomingMessages.with(m);
	}
}
