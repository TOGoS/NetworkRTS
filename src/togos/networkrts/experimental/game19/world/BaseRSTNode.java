package togos.networkrts.experimental.game19.world;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import togos.networkrts.experimental.shape.RectIntersector;
import togos.networkrts.util.BitAddressUtil;

public abstract class BaseRSTNode implements RSTNode
{
	protected final long minBitAddress, maxBitAddress;
	protected final long nextAutoUpdateTime;
	
	protected BaseRSTNode( long minId, long maxId, long nextAutoUpdateTime ) {
		this.minBitAddress = minId; this.maxBitAddress = maxId;
		this.nextAutoUpdateTime = nextAutoUpdateTime;
	}
	
	@Override public long getMinBitAddress() { return minBitAddress; }
	@Override public long getMaxBitAddress() { return maxBitAddress; }
	@Override public long getNextAutoUpdateTime() { return this.nextAutoUpdateTime; }
	
	protected abstract RSTNode _update( int x, int y, int sizePower, long time, Collection<Message> messages, List<Action> results );
	
	@Override public RSTNode update( int x, int y, int sizePower, long time, Collection<Message> messages, List<Action> results ) {
		int relevantMessageCount = 0;
		int size = 1<<sizePower;
		for( Message m : messages ) {
			boolean relevance =
				BitAddressUtil.rangesIntersect(this, m) &&
				m.targetShape.rectIntersection( x, y, size, size ) != RectIntersector.INCLUDES_NONE;
			relevantMessageCount += relevance ? 1 : 0;
		}
		if( relevantMessageCount == 0 ) {
			if( time < nextAutoUpdateTime ) {
				return this;
			}
			messages = Collections.<Message>emptyList();
		}
		// TODO: if relevantMessageCount < messages.length, could filter, here.
		
		return _update( x, y, sizePower, time, messages, results );
	}
}
