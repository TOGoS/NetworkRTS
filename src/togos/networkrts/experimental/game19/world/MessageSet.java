package togos.networkrts.experimental.game19.world;

import java.util.Collections;
import java.util.Iterator;

public interface MessageSet extends Iterable<Message>
{
	public static final MessageSet EMPTY = new MessageSet() {
		@Override public Iterator<Message> iterator() { return Collections.<Message>emptyList().iterator(); }
		@Override public int size() { return 0; }
		@Override public MessageSet subsetApplicableTo(
			double minX, double minY,
			double maxX, double maxY, long minBitAddress, long maxBitAddress
		) {
			return this;
		}
	};
	
	public int size();
	public MessageSet subsetApplicableTo(
		double minX, double minY, double maxX, double maxY,
		long minBitAddress, long maxBitAddress );
}
