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
	
	protected IP6Address getIp6Address() {
		if( !(ipAddress instanceof IP6Address) ) throw new UnsupportedOperationException("Our address is not IPv6!");
		return (IP6Address)ipAddress;
	}
	
	/**
	 * Encodes the Ethernet and IPv6 headers
	 */
	protected int encodeIcmp6ResponseHeaders( PacketWrapping<ICMP6Packet> pw, int icmpLength, byte[] response, int off ) {
		final ICMP6Packet icmp6Packet = pw.payload;
		final IP6Address sourceAddress = icmp6Packet.ip6Packet.getSourceAddress();
		final EthernetFrame ef = (EthernetFrame)pw.parent.parent.payload;
		
		off = EthernetFrame.encodeHeader(ef.getSourceAddress(), ethernetAddress, EtherTypes.IP6, response, off);
		// Apparently hopcount must be 255 for this to be received and processed!
		off = IP6Packet.encodeHeader((byte)0, 0, icmpLength, IPProtocols.ICMP6, (byte)255, getIp6Address(), sourceAddress, response, off);
		return off;
	}
	
	protected void outsendEthernet( byte[] data, int offset, int size, long destBitAddress, String desc ) {
		EthernetFrame outgoingEf = new EthernetFrame( data, 0, data.length );
		System.err.println("Sending Ethernet frame containing "+desc+" to "+destBitAddress);
		network.sendMessage(Message.create(destBitAddress, MessageType.INCOMING_PACKET, bitAddress, outgoingEf));
	}
	
	protected void handleIcmp6Packet( PacketWrapping<ICMP6Packet> pw ) {
		if( !(ipAddress instanceof IP6Address) ) return;
		IP6Address ip6Address = (IP6Address)ipAddress;
		
		final ICMP6Packet icmp6Packet = pw.payload;
		final IP6Address sourceAddress = icmp6Packet.ip6Packet.getSourceAddress();
		final long sourceBitAddress = ((Message)pw.parent.parent.parent.payload).sourceAddress;
		
		final int ethernetIp6HeaderSize = 14+40;
		final byte icmp6Type = icmp6Packet.getType();
		
		switch( icmp6Type ) {
		case ICMP6Packet.NEIGHBOR_SOLICITATION: {
			if( !icmp6Packet.checkNeighborSolicitationTargetAddress(ip6Address) ) return;
			// Otherwise we gotta respond!!!
			
			// FINDINGS (based on pinging from an Ubuntu 12.04 machine
			// and watching the packets go by in Wireshark)
			// ttyl = 64 DOES NOT WORK, even if everything else is right.  255 works.
			// router can be zero, override can be zero.
			
			int icmpLength = ICMP6Packet.NEIGHBOR_ADVERTISEMENT_SIZE;
			
			byte[] response = new byte[ethernetIp6HeaderSize+icmpLength];
			int off = 0;
			off = encodeIcmp6ResponseHeaders( pw, icmpLength, response, off );
			off = ICMP6Packet.encodeNeighborAdvertisement(ip6Address, sourceAddress, ip6Address, isRouter(), true, false, ethernetAddress, response, off);
			outsendEthernet( response, 0, response.length, sourceBitAddress, "ICMPv6 neighbor advertisement" );
			break;
		} case ICMP6Packet.PING6: {
			// See RFC 4443: http://tools.ietf.org/html/rfc4443#page-13
			// Identifier and sequence are arbitrary, so for echo response
			// purposes we'll just treat them as part of the data.
			byte[] response = new byte[ethernetIp6HeaderSize+icmp6Packet.getSize()];
			int off = 0;
			off = encodeIcmp6ResponseHeaders( pw, icmp6Packet.getSize(), response, off );
			off = ICMP6Packet.encodePong(ip6Address, sourceAddress, icmp6Packet, response, off);
			System.err.println("Received ping!");
			outsendEthernet( response, 0, response.length, sourceBitAddress, "ICMPv6 echo response" );
			break;
		} default:
			System.err.println(String.format("Ignoring unsupported ICMPv6 packet type: %02x", icmp6Type));
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
