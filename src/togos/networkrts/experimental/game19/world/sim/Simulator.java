package togos.networkrts.experimental.game19.world.sim;

import java.util.ArrayList;
import java.util.List;

import togos.networkrts.experimental.game19.world.Action;
import togos.networkrts.experimental.game19.world.ActionContext;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.WorldNode;
import togos.networkrts.experimental.shape.RectIntersector;
import togos.networkrts.util.BitAddressUtil;

public class Simulator implements ActionContext
{
	WorldNode rootNode;
	int rootX, rootY, rootSizePower;
	
	public void setRoot( WorldNode n, int x0, int y0, int sizePower ) {
		this.rootNode = n;
		this.rootX = x0;
		this.rootY = y0;
		this.rootSizePower = sizePower;
	}
	
	/**
	 * If null is returned, no update is needed for messages or time.
	 * Otherwise an array of possibly relevant messages is returned. 
	 */
	protected Message[] needsUpdate( WorldNode n, int x, int y, int sizePower, long time, Message[] messages ) {
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
	
	public void update( long time ) {
		Message[] messages = incomingMessages.toArray(new Message[incomingMessages.size()]);
		incomingMessages.clear();
		
		List<Action> actions = new ArrayList<Action>();
		rootNode = rootNode.update( rootX, rootY, rootSizePower, time, messages, actions );
		for( Action act : actions ) act.apply(this);
	}
	
	//// Action context
	
	@Override public WorldNode getRootNode() { return rootNode; }
	@Override public int getRootX() { return rootX; }
	@Override public int getRootY() { return rootY; }
	@Override public int getRootSizePower() { return rootSizePower; }
	
	@Override public void setRootNode( WorldNode n ) {
		rootNode = n;
	}
	
	@Override public void enqueueMessage( Message m ) {
		incomingMessages.add(m);
	}
}
