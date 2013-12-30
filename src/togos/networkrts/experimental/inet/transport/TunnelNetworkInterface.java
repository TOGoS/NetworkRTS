package togos.networkrts.experimental.inet.transport;

import java.util.HashSet;
import java.util.Set;

public class TunnelNetworkInterface<InnerPacket, OuterPacket>
	implements NetworkInterface<InnerPacket>, PacketListener<OuterPacket>
{
	final NetworkInterface<OuterPacket> wrappedInterface;
	final TunnelCodec<InnerPacket, OuterPacket> packetCodec;
	
	protected Set<PacketListener<InnerPacket>> listeners = new HashSet<PacketListener<InnerPacket>>();
	
	public TunnelNetworkInterface(
		NetworkInterface<OuterPacket> wrappedInterface,
		TunnelCodec<InnerPacket, OuterPacket> packetCodec
	) {
		this.wrappedInterface = wrappedInterface;
		this.packetCodec = packetCodec;
		wrappedInterface.addIncomingPacketListener(this);
	}
	
	@Override public void sendPacket(InnerPacket ip) {
		wrappedInterface.sendPacket(packetCodec.encode(ip));
	}
	
	@Override public void addIncomingPacketListener(PacketListener<InnerPacket> pl) {
		synchronized( listeners ) {
			listeners.add(pl);
		}
	}
	
	@Override public void packetReceived(OuterPacket op) {
		synchronized( listeners ) {
			if( listeners.size() == 0 ) return;
		}
		
		InnerPacket ip = packetCodec.decode(op);
		// TODO: these synchronized blocks are probably sub-optimal
		synchronized( listeners ) {
			for( PacketListener<InnerPacket> l : listeners ) {
				l.packetReceived(ip);
			}
		}
	};
}
