package togos.networkrts.experimental.game19.world.sim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import togos.networkrts.experimental.game19.world.Action;
import togos.networkrts.experimental.game19.world.ActionContext;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.EntityRanges;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.experimental.gameengine1.index.EntityUpdater;
import togos.networkrts.experimental.shape.RectIntersector;
import togos.networkrts.util.BitAddressUtil;

public class Simulator implements ActionContext
{
	World world;
	
	public void setRoot(World world) {
		this.world = world;
	}
	
	/**
	 * If null is returned, no update is needed for messages or time.
	 * Otherwise an array of possibly relevant messages is returned. 
	 */
	protected Message[] needsUpdate( RSTNode n, int x, int y, int sizePower, long time, Message[] messages ) {
		int relevantMessageCount = 0;
		int size = 1<<sizePower;
		for( Message m : messages ) {
			boolean relevance =
				BitAddressUtil.rangesIntersect(n, m) &&
				m.targetShape.rectIntersection( x, y, size, size ) != RectIntersector.INCLUDES_NONE;
			relevantMessageCount += relevance ? 1 : 0;
		}
		if( relevantMessageCount == 0 ) {
			return time < n.getNextAutoUpdateTime() ? null : Message.EMPTY_LIST;
		} else {
			// TODO: if some are relevant but others not, could filter here 
			return messages;
		}
	}
	
	List<Message> incomingMessages = new ArrayList<Message>();
	
	Random r = new Random();
	protected NonTile updateNonTile( NonTile nt, long time, World w, Collection<Message> incomingMessages, Collection<NonTile> generatedNonTiles, Collection<Message> generatedMessages ) {
		return nt.withPosition( time, nt.x + r.nextGaussian(), nt.y + r.nextGaussian() );
	}
	
	protected EntitySpatialTreeIndex<NonTile> updateNonTiles( final World w, final long time, final Collection<Message> incomingMessages, final Collection<Message> generatedMessages ) {
		return world.nonTiles.updateEntities(EntityRanges.BOUNDLESS, new EntityUpdater<NonTile>() {
			// TODO: this is very unoptimized
			// it doesn't take advantage of the tree structure at all
			// for determining what messages need to be delivered, etc
			// which could be a problem if there are lots of entities * lots of messages
			@Override
			public NonTile update(NonTile nt, Collection<NonTile> generatedNonTiles) {
				return updateNonTile(nt, time, w, incomingMessages, generatedNonTiles, generatedMessages);
			}
		});
	}
	
	protected void update( long time, Collection<Message> incomingMessages ) {
		List<Message> newMessages = new ArrayList<Message>();
		List<Action> actions = new ArrayList<Action>(); // TODO: remove
		do {
			int rstSize = 1<<world.rstSizePower;
			world = new World(
				world.rst.update( -rstSize/2, -rstSize/2, world.rstSizePower, time, incomingMessages, actions ),
				world.rstSizePower,
				updateNonTiles(world, time, incomingMessages, newMessages)
			);
			incomingMessages = newMessages;
			newMessages = new ArrayList<Message>();
		} while( incomingMessages.size() > 0 );
		for( Action act : actions ) act.apply(this);
	}
	
	public void update( long time) {
		// TODO: This is not thread-safe at all!
		// In case someone's thinking of adding incoming messages
		// e.g. as they are received from a socket.
		// I expect this to be completely rewritten, anyway.
		update( time, incomingMessages );
		incomingMessages.clear();
	}
	
	//// Action context
	// TODO: Delete action context carp
	
	@Override public World getWorld() { return world; }
	@Override public void setWorld( World w ) {
		world = w; }
	
	@Override public void enqueueMessage( Message m ) {
		incomingMessages.add(m);
	}
}
