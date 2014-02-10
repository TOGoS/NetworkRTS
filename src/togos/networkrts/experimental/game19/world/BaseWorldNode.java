package togos.networkrts.experimental.game19.world;

import java.util.List;

import togos.networkrts.experimental.game18.sim.IDUtil;
import togos.networkrts.experimental.shape.RectIntersector;

public abstract class BaseWorldNode implements WorldNode
{
	protected final long minId, maxId;
	protected final long nextAutoUpdateTime;
	
	protected BaseWorldNode( long minId, long maxId, long nextAutoUpdateTime ) {
		this.minId = minId; this.maxId = maxId;
		this.nextAutoUpdateTime = nextAutoUpdateTime;
	}
	
	@Override public long getMinId() { return minId; }
	@Override public long getMaxId() { return minId; }
	@Override public long getNextAutoUpdateTime() { return this.nextAutoUpdateTime; }
	
	protected abstract WorldNode _update( int x, int y, int sizePower, long time, Message[] messages, List<Action> results );
	
	@Override public WorldNode update( int x, int y, int sizePower, long time, Message[] messages, List<Action> results ) {
		int relevantMessageCount = 0;
		int size = 1<<sizePower;
		for( Message m : messages ) {
			boolean relevance =
				IDUtil.rangesIntersect(minId, maxId, m.minId, m.maxId) &&
				m.targetShape.rectIntersection( x, y, size, size ) != RectIntersector.INCLUDES_NONE;
			relevantMessageCount += relevance ? 1 : 0;
		}
		if( relevantMessageCount == 0 ) {
			if( time < nextAutoUpdateTime ) {
				return this;
			}
			messages = Message.EMPTY_LIST;
		}
		// TODO: if relevantMessageCount < messages.length, could filter, here.
		
		return _update( x, y, sizePower, time, messages, results );
	}
}
