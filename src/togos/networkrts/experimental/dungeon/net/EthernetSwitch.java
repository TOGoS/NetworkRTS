package togos.networkrts.experimental.dungeon.net;

import java.util.HashMap;

public class EthernetSwitch
{
	static class Port implements EthernetPort {
		public final EthernetSwitch swich;
		public final int number;
		public EthernetPort facing;
		
		public Port( EthernetSwitch swich, int number ) {
			this.swich = swich;
			this.number = number;
		}
		
		/**
		 * Handle an incoming packet.
		 */
		@Override public void put(long time, ObjectEthernetFrame<?> f) {
			swich.handlePacket(number, time, f);
			// TODO Auto-generated method stub
		}
		
		protected void send(long time, ObjectEthernetFrame<?> f) {
			if( facing != null ) facing.put(time, f);
		}
	}
	
	protected final Port[] ports;
	protected final HashMap<Long,Integer> origins = new HashMap<Long,Integer>();	
	
	public EthernetSwitch( int portCount ) {
		ports = new Port[portCount];
		for( int i=0; i<portCount; ++i ) ports[i] = new Port(this, i);
	}
	
	public EthernetPort getPort( int n ) {
		assert n >= 0;
		assert n < ports.length;
		return ports[n];
	}
	
	public void handlePacket(int sourcePortNumber, long time, ObjectEthernetFrame<?> f) {
		Long sourceAddress = Long.valueOf(f.destAddress);
		Long destAddress = Long.valueOf(f.destAddress);
		origins.put( sourceAddress, sourcePortNumber );
		Integer destPortNumber = origins.get(destAddress);
		if( destPortNumber == null ) {
			// TODO: loop detection, ack!
			for( Port p : ports ) {
				p.send(time, f);
			}
		} else {
			ports[destPortNumber.intValue()].send(time, f);
		}
	}
}
