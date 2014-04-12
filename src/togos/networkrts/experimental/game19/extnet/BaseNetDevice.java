package togos.networkrts.experimental.game19.extnet;

import java.util.HashMap;

import togos.networkrts.experimental.game19.util.MessageSender;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.packet19.DataPacket;
import togos.networkrts.experimental.packet19.EtherTypes;
import togos.networkrts.experimental.packet19.EthernetFrame;
import togos.networkrts.experimental.packet19.ICMP6Packet;
import togos.networkrts.experimental.packet19.IP6Address;
import togos.networkrts.experimental.packet19.IP6Packet;
import togos.networkrts.experimental.packet19.IPAddress;
import togos.networkrts.experimental.packet19.IPPacket;
import togos.networkrts.experimental.packet19.IPProtocols;
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
	
	protected boolean isRouter() {
		return false;
	}
	
	protected void handleIcmp6Packet( PacketWrapping<ICMP6Packet> pw ) {
		if( !(ipAddress instanceof IP6Address) ) return;
		IP6Address ip6Address = (IP6Address)ipAddress;
		
		final ICMP6Packet icmp6Packet = pw.payload;
		final IP6Address sourceAddress = icmp6Packet.ip6Packet.getSourceAddress();
		final EthernetFrame ef = (EthernetFrame)pw.parent.parent.payload;
		final Message m = (Message)pw.parent.parent.parent.payload;
		
		switch( icmp6Packet.getType() ) {
		case ICMP6Packet.NEIGHBOR_SOLICITATION:
			if( !icmp6Packet.checkNeighborSolicitationTargetAddress(ip6Address) ) return;
			// Otherwise we gotta respond!!!
			
			// FINDINGS
			// ttyl = 64 DOES NOT WORK, even if everything else is right.  255 works.
			// router can be zero, override can be zero.
			
			int icmpLength = ICMP6Packet.NEIGHBOR_ADVERTISEMENT_SIZE;
			
			byte[] response = new byte[14+40+icmpLength];
			int off = 0;
			off = EthernetFrame.encodeHeader(ef.getSourceAddress(), ethernetAddress, EtherTypes.IP6, response, off);
			// Apparently hopcount must be 255 for this to be received and processed!
			off = IP6Packet.encodeHeader((byte)0, 0, icmpLength, IPProtocols.ICMP6, (byte)255, ip6Address, sourceAddress, response, off);
			off = ICMP6Packet.encodeNeighborAdvertisement(ip6Address, sourceAddress, ip6Address, isRouter(), true, false, ethernetAddress, response, off);
			EthernetFrame outgoingEf = new EthernetFrame( response, 0, response.length );
			System.err.println("Sending message with EthernetFrame back to "+m.sourceAddress);
			network.sendMessage(Message.create(m.sourceAddress, MessageType.INCOMING_PACKET, bitAddress, outgoingEf));
			break;
		case ICMP6Packet.PING6:
			
		}
	}
	
	protected void handleIpPacket( PacketWrapping<IPPacket> pw ) {
		final IPPacket ipp = pw.payload;
		if( !ipp.getDestinationAddress().matches(ipAddress) ) return;
		
		DataPacket ipPayload = ipp.getPayload();
		if( ipPayload instanceof UDPPacket ) {
			handleUdpPacket( new PacketWrapping<UDPPacket>( pw, (UDPPacket)ipPayload ));
		} else if( ipPayload instanceof ICMP6Packet ) {
			handleIcmp6Packet( new PacketWrapping<ICMP6Packet>( pw, ((ICMP6Packet)ipPayload) ));
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
