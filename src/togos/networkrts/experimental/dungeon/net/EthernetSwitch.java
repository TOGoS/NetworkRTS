package togos.networkrts.experimental.dungeon.net;

import java.util.HashMap;

import togos.networkrts.experimental.dungeon.DungeonGame.InternalUpdater;

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
		@Override public void messageReceived(ObjectEthernetFrame<?> f) {
			swich.handlePacket(number, f);
		}
		
		protected void send(ObjectEthernetFrame<?> f) {
			// TODO: Probably want to add some delay
			if( facing != null ) facing.messageReceived(f);
		}
	}
	
	protected final Port[] ports;
	protected final HashMap<Long,Integer> origins = new HashMap<Long,Integer>();
	protected final InternalUpdater updater;
	
	public EthernetSwitch( int portCount, InternalUpdater updater ) {
		ports = new Port[portCount];
		for( int i=0; i<portCount; ++i ) ports[i] = new Port(this, i);
		this.updater = updater;
	}
	
	public EthernetPort getPort( int n ) {
		assert n >= 0;
		assert n < ports.length;
		return ports[n];
	}
	
	public void handlePacket(int sourcePortNumber, ObjectEthernetFrame<?> f) {
		Long sourceAddress = Long.valueOf(f.destAddress);
		Long destAddress = Long.valueOf(f.destAddress);
		origins.put( sourceAddress, sourcePortNumber );
		Integer destPortNumber = origins.get(destAddress);
		if( destPortNumber == null ) {
			// TODO: loop detection, ack!
			for( Port p : ports ) {
				p.send(f);
			}
		} else {
			ports[destPortNumber.intValue()].send(f);
		}
	}
}
