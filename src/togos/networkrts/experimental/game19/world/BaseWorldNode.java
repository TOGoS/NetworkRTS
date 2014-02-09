package togos.networkrts.experimental.game19.world;

import java.util.List;

import togos.networkrts.experimental.game18.sim.IDUtil;

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
	
	protected abstract WorldNode _update( int x, int y, int size, long time, Message[] messages, List<Action> results );
	
	@Override public WorldNode update( int x, int y, int size, long time, Message[] messages, List<Action> results ) {
		boolean anyMessagesRelevant = false;
		for( Message m : messages ) {
			if( !IDUtil.rangesIntersect(minId, maxId, m.minId, m.maxId) ) continue;
			if( m.maxX <= x || m.maxY <= y || m.minX >= x+size || m.minY >= y+size ) continue;
			anyMessagesRelevant = true;
		}
		if( !anyMessagesRelevant ) {
			if( time < nextAutoUpdateTime ) {
				return this;
			}
			messages = Message.EMPTY_LIST;
		}
		
		return _update( x, y, size, time, messages, results );
	}
}
