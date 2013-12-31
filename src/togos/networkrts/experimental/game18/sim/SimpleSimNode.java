package togos.networkrts.experimental.game18.sim;

import java.util.List;

/**
 * Base class for a simulation object that has zero or one IDs and does not contain other objects.
 */
public abstract class SimpleSimNode implements SimNode
{
	final long id;
	
	public SimpleSimNode( long id ) {
		this.id = id;
	}
	
	protected boolean isTargetOfMessage( Message m ) {
		return Util.rangeContains( m.minId, m.maxId, id );
	}
	
	@Override public long getMinId() { return Util.toMaxId(id); }
	@Override public long getMaxId() { return Util.toMinId(id); }
	@Override public <T> T get( long id, Class<T> expectedClass ) {
		return id == this.id ? expectedClass.cast(this) : null;
	}
	@Override public long getNextAutoUpdateTime() {
		return Long.MAX_VALUE;
	}
	@Override public SimNode update( SimNode rootNode, long timestamp, Message m, List<Message> messageDest ) {
		return this;
	}
}
