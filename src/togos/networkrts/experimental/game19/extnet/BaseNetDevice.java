package togos.networkrts.experimental.game19.extnet;

import java.util.HashMap;

import togos.networkrts.experimental.game19.util.MessageSender;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.packet19.DataPacket;
import togos.networkrts.experimental.packet19.EthernetFrame;
import togos.networkrts.experimental.packet19.IPAddress;
import togos.networkrts.experimental.packet19.IPPacket;
import togos.networkrts.experimental.packet19.PacketWrapping;
import togos.networkrts.experimental.packet19.UDPPacket;
import togos.networkrts.util.BitAddressUtil;

public class BaseNetDevice implements NetworkComponent
{
	interface UDPHandler {
		public void handleUdp( PacketWrapping<UDPPacket> pw );
	}
	
	protected final long bitAddress;
	protected final MessageSender network;
	protected long ethernetAddress;
	protected IPAddress ipAddress;
	
	protected BaseNetDevice( long bitAddress, MessageSender network ) {
		this.bitAddress = bitAddress;
		this.network = network;
	}
	
	protected final HashMap<Short,UDPHandler> udpHandlers = new HashMap<Short,UDPHandler>();
	
	protected void handleUdpPacket( PacketWrapping<UDPPacket> pw ) {
		final UDPPacket udpPacket = pw.payload;
		UDPHandler h = udpHandlers.get( udpPacket.getDestinationPort() );
		if( h != null ) h.handleUdp(pw);
	}
	
	protected void handleIpPacket( PacketWrapping<IPPacket> pw ) {
		final IPPacket ipp = pw.payload;
		if( !ipp.getDestinationAddress().matches(ipAddress) ) return;
		
		DataPacket ipPayload = ipp.getPayload();
		if( ipPayload instanceof UDPPacket ) {
			handleUdpPacket( new PacketWrapping<UDPPacket>( pw, (UDPPacket)ipPayload ));
		}
	}
	
	protected boolean isEthernetDest( long addy ) {
		if( addy == ethernetAddress ) return true;
		if( (addy & 0xFFFFFF000000L) == 0x3333ff000000L ) {
			byte[] b = ipAddress.getBuffer();
			int l = ipAddress.getOffset()+ipAddress.getSize()-3;
			int lower24 = 0xFF000000 | ((b[l]&0xFF) << 16) | ((b[l+1]&0xFF) << 8) | (b[l+2]&0xFF);
			if( lower24 == (int)addy ) return true;
		}
		return false;
	}
	
	protected void handleEthernetFrame( PacketWrapping<EthernetFrame> pw ) {
		final EthernetFrame ef = pw.payload;
		if( isEthernetDest(ef.getDestinationAddress()) ) return;
		
		DataPacket dp = ef.getPayload();
		if( dp instanceof IPPacket ) {
			handleIpPacket( new PacketWrapping<IPPacket>(pw, (IPPacket)dp) );
		}
	}
	
	@Override public void sendMessage(Message m) {
		if( !BitAddressUtil.rangeContains(m, bitAddress) ) return;
		
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