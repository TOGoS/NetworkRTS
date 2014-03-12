package togos.networkrts.experimental.game19.sim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.EntityRanges;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.experimental.gameengine1.index.EntityUpdater;
import togos.networkrts.experimental.gensim.AutoEventUpdatable2;

/**
 * The pure-ish, non-threaded part of the simulator
 */
public class Simulation implements AutoEventUpdatable2<Message>
{
	static class ArrayMessageSet extends ArrayList<Message> implements MessageSet, MessageSender
	{
		private static final long serialVersionUID = 1L;
		
		public static final MessageSet getMessageSet( Collection<Message> messages ) {
			return messages.size() == 0 ? MessageSet.EMPTY : new ArrayMessageSet(messages);
		}
		
		public ArrayMessageSet() { super(); }
		public ArrayMessageSet(Collection<Message> m) { super(m); }
		
		@Override public MessageSet subsetApplicableTo( double minX, double minY, double maxX, double maxY, long minBitAddress, long maxBitAddress ) {
			// TODO?
			return this;
		}
		
		@Override public synchronized void sendMessage( Message m ) {
			add(m);
		}
	}
	
	static class NNTLNonTileUpdateContext implements NonTileUpdateContext {
		protected final Collection<NonTile> nonTileList;
		protected final UpdateContext updateContext;
		
		private NNTLNonTileUpdateContext( UpdateContext updateContext, Collection<NonTile> nonTileList ) {
			this.updateContext = updateContext;
			this.nonTileList = nonTileList;
		}
		
		public static NNTLNonTileUpdateContext get( NNTLNonTileUpdateContext oldInstance, UpdateContext updateContext, Collection<NonTile> nonTileList ) {
			return oldInstance == null || oldInstance.nonTileList != nonTileList ?
				new NNTLNonTileUpdateContext(updateContext, nonTileList) : oldInstance;
		}
		
		@Override public void sendMessage( Message m ) { updateContext.sendMessage(m); }
		@Override public void startAsyncTask( AsyncTask at ) { updateContext.startAsyncTask(at); }
		@Override public void addNonTile( NonTile nt ) { nonTileList.add(nt); }
	}
	
	protected World world;
	protected long time = 0;
	/** Tasks to be done later will be sent here! */
	protected final LinkedBlockingQueue<AsyncTask> asyncTaskQueue;
	/** Messages to things outside the simulation go here! */
	protected final LinkedBlockingQueue<Message> outgoingMessageQueue;
	
	class SimUpdateContext implements UpdateContext {
		public final ArrayMessageSet newMessages = new ArrayMessageSet();
		
		@Override public void sendMessage( Message m ) {
			if( (m.minBitAddress & BitAddresses.TYPE_EXTERNAL) == 0 ) {
				newMessages.add(m);
			}
			if( (m.maxBitAddress & BitAddresses.TYPE_EXTERNAL) == BitAddresses.TYPE_EXTERNAL ) {
				outgoingMessageQueue.add(m);
			}
		}
		@Override public void startAsyncTask( AsyncTask at ) {
			asyncTaskQueue.add(at);
		}
	}
	
	public Simulation(World world, LinkedBlockingQueue<AsyncTask> asyncTaskQueue, LinkedBlockingQueue<Message> outgoingMessageQueue ) {
		this.world = world;
		this.asyncTaskQueue = asyncTaskQueue;
		this.outgoingMessageQueue = outgoingMessageQueue;
	}
	
	public World getWorld() {
		return world;
	}
	
	protected NonTile updateNonTile( NonTile nt, long time, World w, MessageSet incomingMessages, NonTileUpdateContext updateContext, int phase ) {
		switch(phase) {
		case 1: return nt.withUpdatedPosition(time);
		case 2: return nt.behavior.update( nt, time, w, incomingMessages, updateContext );
		}
		throw new RuntimeException("Invalid phase "+phase);
	}
	
	protected EntitySpatialTreeIndex<NonTile> updateNonTiles( final World w, final long time, final MessageSet incomingMessages, final UpdateContext updateContext, final int phase ) {
		return world.nonTiles.updateEntities(EntityRanges.BOUNDLESS, new EntityUpdater<NonTile>() {
			// TODO: this is very unoptimized
			// it doesn't take advantage of the tree structure at all
			// for determining what messages need to be delivered, etc
			// which could be a problem if there are lots of entities * lots of messages
			
			NNTLNonTileUpdateContext nntlntuc;
			
			@Override public NonTile update(NonTile nt, Collection<NonTile> generatedNonTiles) {
				return updateNonTile(
					nt, time, w, incomingMessages,
					NNTLNonTileUpdateContext.get(nntlntuc, updateContext, generatedNonTiles), phase);
			}
		});
	}
	
	protected void update( long time, MessageSet incomingMessages ) {
		assert time < Long.MAX_VALUE;
		
		do {
			SimUpdateContext updateContext = new SimUpdateContext();
			int rstSize = 1<<world.rstSizePower;
			world = new World(
				world.rst.update( -rstSize/2, -rstSize/2, world.rstSizePower, time, incomingMessages, updateContext ),
				world.rstSizePower,
				updateNonTiles(world, time, incomingMessages, updateContext, 1)
			);
			world = new World(
				world.rst, world.rstSizePower,
				updateNonTiles(world, time, incomingMessages, updateContext, 2)
			);
			incomingMessages = updateContext.newMessages;
			this.time = time;
		} while( incomingMessages.size() > 0 );
	}
	
	public Simulation update( long time, Collection<Message> events ) {
		update( time, ArrayMessageSet.getMessageSet(events) );
		return this;
	}

	@Override public long getNextAutoUpdateTime() {
		return time+1; //world.getNextAutoUpdateTime();
	}
	
	@Override public long getCurrentTime() { return time; }
}
