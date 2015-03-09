package togos.networkrts.experimental.game18.sim;

import java.util.List;

import togos.networkrts.util.BitAddressUtil;

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
		return BitAddressUtil.rangeContains( m, id );
	}
	
	@Override public long getMinId() { return BitAddressUtil.toMaxAddress(id); }
	@Override public long getMaxId() { return BitAddressUtil.toMinAddress(id); }
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
