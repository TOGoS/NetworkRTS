package togos.networkrts.experimental.game19.sim;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

import togos.networkrts.experimental.game19.world.ArrayMessageSet;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.Messages;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.EntityAggregation;
import togos.networkrts.experimental.gameengine1.index.EntityRanges;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.experimental.gameengine1.index.EntityUpdater;
import togos.networkrts.experimental.gensim.AutoEventUpdatable2;

/**
 * The pure-ish, non-threaded part of the simulator
 */
public class Simulation implements AutoEventUpdatable2<Message>
{
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
	
	protected boolean needsUpdate( long time, int phase, EntityAggregation er, MessageSet messages ) {
		return
			(phase == 2 && Messages.isApplicableTo(messages, er)) ||
			(er.getNextAutoUpdateTime() <= time && BitAddresses.containsFlag(er.getMaxBitAddress(), BitAddresses.phaseUpdateFlag(phase)));
	}
	
	protected EntitySpatialTreeIndex<NonTile> updateNonTiles( final World w, final long time, final MessageSet incomingMessages, final UpdateContext updateContext, final int phase ) {
		return world.nonTiles.updateEntities(EntityRanges.BOUNDLESS, new EntityUpdater<NonTile>() {
			// TODO: this is very unoptimized
			// it doesn't take advantage of the tree structure at all
			// for determining what messages need to be delivered, etc
			// which could be a problem if there are lots of entities * lots of messages
			
			NNTLNonTileUpdateContext nntlntuc;
			
			@Override public NonTile update(NonTile nt, Collection<NonTile> generatedNonTiles) {
				return nt.update( time, phase, w, incomingMessages,
					NNTLNonTileUpdateContext.get(nntlntuc, updateContext, generatedNonTiles));
			}
		});
	}
	
	protected World update( long time, int phase, World world, MessageSet incomingMessages, SimUpdateContext updateContext ) {
		// Update RST when phase = 2
		RSTNode rst;
		int rstSizePower;
		if( phase == 2 ) {
			int rstSize = 1<<world.rstSizePower;
			rst = world.rst.update( -rstSize/2, -rstSize/2, world.rstSizePower, time, incomingMessages, updateContext );
			rstSizePower = world.rstSizePower;
		} else {
			rst = world.rst;
			rstSizePower = world.rstSizePower;
		}
		
		return new World(
			rst, rstSizePower,
			updateNonTiles(world, time, incomingMessages, updateContext, phase),
			world.background
		);
	}
	
	protected void update( long time, MessageSet incomingMessages ) {
		System.err.println("Updating to "+time);
		
		assert time < Long.MAX_VALUE;
		
		if( needsUpdate(time, 1, world, MessageSet.EMPTY) ) {
			SimUpdateContext updateContext = new SimUpdateContext();
			world = update(time, 1, world, MessageSet.EMPTY, updateContext);
			incomingMessages = Messages.union(incomingMessages, updateContext.newMessages);
		}
		while( needsUpdate(time, 2, world, incomingMessages) ) {
			SimUpdateContext updateContext = new SimUpdateContext();
			world = update(time, 2, world, incomingMessages, updateContext);
			incomingMessages = updateContext.newMessages;
			this.time = time;
		}
	}
	
	public Simulation update( long time, Collection<Message> events ) {
		update( time, ArrayMessageSet.getMessageSet(events) );
		return this;
	}
	
	@Override public long getNextAutoUpdateTime() {
		return world.getNextAutoUpdateTime();
	}
	
	@Override public long getCurrentTime() { return time; }
}
