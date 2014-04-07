package togos.networkrts.experimental.game19.extnet;

import java.util.HashMap;

import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.packet19.DataPacket;
import togos.networkrts.experimental.packet19.EthernetFrame;
import togos.networkrts.experimental.packet19.IP6Address;
import togos.networkrts.experimental.packet19.IPPacket;
import togos.networkrts.experimental.packet19.MalformedDataException;
import togos.networkrts.experimental.packet19.PacketWrapping;
import togos.networkrts.experimental.packet19.UDPPacket;

public class BaseNetDevice implements NetworkComponent
{
	interface UDPHandler {
		public void handleUdp( PacketWrapping<UDPPacket> pw );
	}
	
	protected long ethernetAddress;
	protected IP6Address ipAddress;
	
	protected final HashMap<Short,UDPHandler> udpHandlers = new HashMap<Short,UDPHandler>();
	
	protected void handleUdpPacket( PacketWrapping<UDPPacket> pw ) {
		final UDPPacket udpPacket = pw.payload;
		UDPHandler h = udpHandlers.get( udpPacket.getDestinationPort() );
		if( h != null ) h.handleUdp(pw);
	}
	
	protected void handleIpPacket( PacketWrapping<IPPacket> pw ) {
		final IPPacket ipp = pw.payload;
		if( !ipp.getDestinationAddress().equals(ipAddress) ) return;
		
		DataPacket ipPayload = ipp.getPayload();
		if( ipPayload instanceof UDPPacket ) {
			handleUdpPacket( new PacketWrapping<UDPPacket>( pw, (UDPPacket)ipPayload ));
		}
	}
	
	protected void handleEthernetFrame( PacketWrapping<EthernetFrame> pw ) {
		final EthernetFrame ef = pw.payload;
		if( ef.getDestinationAddress() != ethernetAddress ) return;
		
		IPPacket ipp;
		try {
			ipp = ef.getPayload().getPayload(IPPacket.class, IPPacket.CODEC);
		} catch( MalformedDataException e ) {
			System.err.println("Received ethernet frame with invalid IP packet");
			e.printStackTrace();
			return;
		}
		
		handleIpPacket(new PacketWrapping<IPPacket>(pw, ipp));
	}
	
	@Override public void sendMessage(Message m) {
		switch( m.type ) {
		case INCOMING_PACKET:
			if( m.payload instanceof EthernetFrame ) {
				PacketWrapping<Message> pw = new PacketWrapping<Message>(m);
				handleEthernetFrame( new PacketWrapping<EthernetFrame>(pw, (EthernetFrame)m.payload) );
			}
			break;
		default:
			// Nada
		}
	}
	
	@Override public void start() { }
	@Override public void setDaemon(boolean d) { }
	@Override public void halt() { }
}
