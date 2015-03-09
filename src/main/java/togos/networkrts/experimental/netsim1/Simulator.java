package togos.networkrts.experimental.netsim1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import togos.blob.ByteChunk;
import togos.blob.util.SimpleByteChunk;
import togos.networkrts.experimental.gensim.EventLoop;
import togos.networkrts.experimental.gensim.EventUpdatable;
import togos.networkrts.experimental.gensim.TimedEventQueue;

public class Simulator implements EventUpdatable<Simulator.SimulatorEvent>
{
	TimedEventQueue<SimulatorEvent> teq = new TimedEventQueue<SimulatorEvent>();
	
	interface HostAction {
		public void apply( Simulator s, String hostId );
	}
	
	static class SendPacketAction implements HostAction {
		public final String interfaceId;
		public final ByteChunk payload; // Will change at some point
		public SendPacketAction( String interfaceId, ByteChunk payload ) {
			this.interfaceId = interfaceId;
			this.payload = payload;
		}
		
		public void apply( Simulator s, String hostId ) {
			s.sendPacket( hostId, interfaceId, payload );
		}
	}
		
	static class HostBehaviorResult {
		public final HostBehavior newBehavior;
		public final List<HostAction> actions;
		public HostBehaviorResult( HostBehavior newState, List<HostAction> actions ) {
			this.newBehavior = newState;
			this.actions = actions;
		}
	}
	
	interface HostBehavior {
		public HostBehaviorResult packetReceived( String interfaceId, ByteChunk payload );
	}
	
	static class NullHostBehavior implements HostBehavior {
		public HostBehaviorResult packetReceived( String interfaceId, ByteChunk payload ) {
			return new HostBehaviorResult( this, Collections.<HostAction>emptyList() );
		}
	}
	
	static class Link {
		long delay;
		String host1, if1, host2, if2;
	}
	
	static class Host {
		public HostBehavior behavior = new NullHostBehavior();
		public HashMap<String,String> interfaceLinkIds = new HashMap<String,String>();
	}
	
	interface SimulatorEvent {
		public void run( Simulator s );
	}
	
	protected HashMap<String,Host> hosts = new HashMap<String,Host>();
	protected HashMap<String,Link> links = new HashMap<String,Link>();
	
	int nextLinkId = 1;
	int nextHostId = 1;
	
	protected String newLinkId() {
		return "link"+(nextLinkId++);
	}
	
	protected String newHostId() {
		return "host"+(nextLinkId++);
	}
	
	protected void disconnectInterface( String hostId, String ifId ) {
		Host h = hosts.get( hostId );
		if( h == null ) return;
		String linkId = h.interfaceLinkIds.get(ifId);
		if( linkId == null ) return;
		Link l = links.get(linkId);
		if( l != null ) {
			if( hostId.equals(l.host1) && ifId.equals(l.if1) ) {
				l.host1 = null;
				l.if1   = null;
			}
			if( hostId.equals(l.host2) && ifId.equals(l.if2) ) {
				l.host2 = null;
				l.if2   = null;
			}
		}
		h.interfaceLinkIds.remove(ifId);
	}
	
	protected boolean connectInterface( String hostId, String ifId, String linkId ) {
		disconnectInterface( hostId, ifId );
		
		Host h = hosts.get(hostId);
		if( h == null ) return false;
		
		Link l = links.get(linkId);
		if( l == null ) return false;
		
		if( l.host1 == null ) {
			l.host1 = hostId;
			l.if1 = ifId;
			h.interfaceLinkIds.put(ifId, linkId);
			return true;
		}
		if( l.host2 == null ) {
			l.host2 = hostId;
			l.if2 = ifId;
			h.interfaceLinkIds.put(ifId, linkId);
			return true;
		}
		return false;
	}
	
	protected void connectHosts( String host1, String if1, String host2, String if2, long delay ) {
		Host h1 = hosts.get(host1);
		if( h1 == null ) return;
		
		Link l = new Link();
		l.delay = delay;
		String linkId = newLinkId(); 
		links.put( linkId, l );
		if( host1.equals(host2) && if1.equals(if2) ) {
			// Special case to allow an interface to be connected to itself:
			l.host1 = host1; l.if1 = if1;
			l.host2 = host2; l.if2 = if2;
			disconnectInterface(host1, if1);
			h1.interfaceLinkIds.put(if1, linkId);
		} else {
			connectInterface( host1, if1, linkId );
			connectInterface( host2, if2, linkId );
		}
	}
	
	protected void connectLoopback( String hostId, String ifId, long delay ) {
		connectHosts( hostId, ifId, hostId, ifId, delay );
	}
	
	protected void deliverPacket( String toHostId, String toInterfaceId, ByteChunk packet ) {
		Host h = hosts.get(toHostId);
		if( h == null ) return;
		HostBehaviorResult r = h.behavior.packetReceived(toInterfaceId, packet);
		h.behavior = r.newBehavior;
		for( HostAction a : r.actions ) {
			a.apply( this, toHostId );
		}
	}
	
	// Arbitrary tick interval
	long tickInterval = 10;
	long currentTime = Long.MIN_VALUE;
	
	/** Return the simulation time of the next 'tick' after the given delay */
	public long tickAfterDelay( long delay ) {
		long destTime = currentTime + delay;
		if( destTime % tickInterval > 0 ) {
			// Round up to next tick
			return destTime + tickInterval - (destTime % tickInterval);
		} else {
			return destTime;
		}
	}
	
	protected void sendPacket( String fromHostId, String fromInterfaceId, final ByteChunk packet ) {
		Host h = hosts.get(fromHostId);
		if( h == null ) return;
		
		String linkId = h.interfaceLinkIds.get(fromInterfaceId);
		if( linkId == null ) return;
		
		Link l = links.get(linkId);
		if( l == null ) return;
		
		final String toHostId, toInterfaceId;
		if( fromHostId.equals(l.host1) && fromInterfaceId.equals(l.if1) ) {
			toHostId = l.host2;
			toInterfaceId = l.if2;
		} else {
			toHostId = l.host1;
			toInterfaceId = l.if1;
		}
		
		teq.enqueue( tickAfterDelay(l.delay), new SimulatorEvent() {
			public void run( Simulator s ) {
				s.deliverPacket(toHostId, toInterfaceId, packet);
			}
		});
	}
	
	@Override public Simulator update( long time, SimulatorEvent evt ) {
		if( time < currentTime ) {
			throw new RuntimeException("Tried to rewind time from "+currentTime+" to "+time);
		}
		
		currentTime = time;
		if( evt != null ) evt.run(this);
		return this;
	}
	
	public static void main( String[] args ) throws Exception {
		final Simulator s = new Simulator();
		
		final String hostId = s.newHostId();
		Host h = new Host();
		h.behavior = new HostBehavior() {
			public HostBehaviorResult packetReceived(String interfaceId, ByteChunk payload) {
				List<HostAction> actions = new ArrayList<HostAction>();
				if( interfaceId.startsWith("lo") ) {
					System.err.println( "Got packet on interface " + interfaceId + " ("+System.currentTimeMillis()+"/"+s.currentTime+")");
					actions.add(new SendPacketAction( interfaceId, payload));
				}
				return new HostBehaviorResult(this, actions);
			}
		};
		s.hosts.put(hostId, h);
		s.connectLoopback(hostId, "lo-10ms"  ,   10);
		s.connectLoopback(hostId, "lo-100ms" ,  100);
		s.connectLoopback(hostId, "lo-1000ms", 1000);
		
		s.teq.enqueueImmediate(new SimulatorEvent() {
			public void run( Simulator s ) {
				s.deliverPacket( hostId, "lo-100ms", SimpleByteChunk.EMPTY );
				s.deliverPacket( hostId, "lo-10ms", SimpleByteChunk.EMPTY );
			}
		});
		
		EventLoop.run( s.teq, s );
	}
}
