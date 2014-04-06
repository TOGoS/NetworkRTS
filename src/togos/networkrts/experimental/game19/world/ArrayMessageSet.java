package togos.networkrts.experimental.game19.world;

import java.util.ArrayList;
import java.util.Collection;

import togos.networkrts.experimental.game19.util.MessageSender;
import togos.networkrts.experimental.shape.RectIntersector;
import togos.networkrts.util.BitAddressUtil;

public class ArrayMessageSet extends ArrayList<Message> implements MessageSet, MessageSender
{
	private static final long serialVersionUID = 1L;
	
	public static final MessageSet getMessageSet( Collection<Message> messages ) {
		return messages.size() == 0 ? MessageSet.EMPTY : new ArrayMessageSet(messages);
	}
	
	public ArrayMessageSet() { super(); }
	public ArrayMessageSet(Collection<Message> m) { super(m); }
	
	@Override public MessageSet subsetApplicableTo( double minX, double minY, double maxX, double maxY, long minBitAddress, long maxBitAddress ) {
		int total = size();
		int applicable = 0;
		for( Message m : this ) {
			if( m.targetShape.rectIntersection(minX, minY, maxX, maxY) == RectIntersector.INCLUDES_NONE ) continue;
			if( !BitAddressUtil.rangesIntersect(m.minBitAddress, m.maxBitAddress, minBitAddress, maxBitAddress) ) continue;
			++applicable;
		}
		if( applicable == 0 ) return MessageSet.EMPTY;
		if( applicable == total ) return this;
		if( applicable == 1 ) {
			for( Message m : this ) {
				if( m.targetShape.rectIntersection(minX, minY, maxX, maxY) == RectIntersector.INCLUDES_NONE ) continue;
				if( !BitAddressUtil.rangesIntersect(m.minBitAddress, m.maxBitAddress, minBitAddress, maxBitAddress) ) continue;
				return m;
			}
		}
		
		ArrayMessageSet newMs = new ArrayMessageSet();
		for( Message m : this ) {
			if( m.targetShape.rectIntersection(minX, minY, maxX, maxY) == RectIntersector.INCLUDES_NONE ) continue;
			if( !BitAddressUtil.rangesIntersect(m.minBitAddress, m.maxBitAddress, minBitAddress, maxBitAddress) ) continue;
			newMs.add(m);
		}
		return MessageSet.EMPTY;
	}
	
	@Override public synchronized void sendMessage( Message m ) {
		add(m);
	}
}