package togos.networkrts.experimental.game18.sim;

import java.util.ArrayList;
import java.util.List;

import togos.networkrts.util.BitAddressUtil;

public class SimOrgNode implements SimNode
{
	protected final SimNode[] childs;
	protected final long minId, maxId;
	protected final long nextAutoUpdateTime;
	
	protected final boolean containsId( long id ) {
		return (id | maxId) == maxId && (id & minId) == minId;
	}
	
	public SimOrgNode( SimNode[] childs ) {
		assert childs != null;
		long minId = BitAddressUtil.MAX_ADDRESS;
		long maxId = BitAddressUtil.MIN_ADDRESS;
		long nextAutoUpdateTime = Long.MAX_VALUE;
		this.childs = childs;
		for( SimNode n : childs ) {
			minId &= n.getMinId();
			maxId |= n.getMaxId();
			long aut = n.getNextAutoUpdateTime();
			if( aut < nextAutoUpdateTime ) nextAutoUpdateTime = aut;
		}
		this.minId = minId;
		this.maxId = maxId;
		this.nextAutoUpdateTime = nextAutoUpdateTime;
	}
		
	@Override public SimOrgNode update( SimNode rootNode, long timestamp, Message m, List<Message> messageDest ) {
		if( timestamp < nextAutoUpdateTime && !BitAddressUtil.rangesIntersect(m, minId, maxId) ) return this;
		
		for( int i=0; i<childs.length; ++i ) {
			SimNode newChild = childs[i].update( rootNode, timestamp, m, messageDest );
			if( newChild != childs[i] ) {
				// Something changed!
				List<SimNode> newChilds = new ArrayList<SimNode>(childs.length);
				
				for( int j=0; j<i; ++j ) newChilds.add( childs[j] );
				
				if( newChild != null ) newChilds.add(newChild);
				
				for( int j=i+1; j<childs.length; ++j ) {
					if( (newChild = childs[j].update( rootNode, timestamp, m, messageDest )) != null ) {
						newChilds.add(newChild);
					}
				}
				
				return new SimOrgNode(newChilds.toArray(new SimNode[newChilds.size()]));
			}
		}
		return this;
	}
	
	@Override public long getNextAutoUpdateTime() { return nextAutoUpdateTime; }
	@Override public long getMinId() { return minId; }
	@Override public long getMaxId() { return maxId; }

	@Override public <T> T get( long id, Class<T> expectedClass ) {
		if( !BitAddressUtil.rangeContains(minId, maxId, id) ) return null;
		
		for( SimNode n : childs ) {
			T t = n.get(id, expectedClass);
			if( t != null ) return t;
		}
		
		return null;
	}
}
