package togos.networkrts.experimental.dungeon.net;

import java.util.HashMap;

import togos.networkrts.experimental.dungeon.DungeonGame.InternalUpdater;

public class EthernetSwitch
{
	@SuppressWarnings("rawtypes")
	static class Port extends AbstractConnector<ObjectEthernetFrame> {
		public final EthernetSwitch swich;
		public final int number;
		
		public Port( EthernetSwitch swich, int number ) {
			super( ConnectorTypes.rj45.female, ObjectEthernetFrame.class );
			this.swich = swich;
			this.number = number;
		}
		
		@Override public void messageReceived(ObjectEthernetFrame f) {
			swich.handlePacket(number, f);
		}
		
		@Override public boolean isLocked() {
			return false;
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
	
	public Port getPort( int n ) {
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
				p.sendMessage(f);
			}
		} else {
			ports[destPortNumber.intValue()].sendMessage(f);
		}
	}
}
