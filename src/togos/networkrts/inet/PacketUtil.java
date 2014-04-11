package togos.networkrts.inet;

import java.io.PrintStream;

import togos.networkrts.util.ByteUtil;

public class PacketUtil {
	
	// Based on
	// http://support.novell.com/techcenter/articles/img/nc1999_0502.gif
	// http://routemyworld.com/wp-content/uploads/2009/01/ipv6header.png
	
	public static final int IP4_HEADER_SIZE = 20;
	public static final int IP6_HEADER_SIZE = 40;
	public static final int ICMP_HEADER_SIZE = 8;
	
	public static int getIp6PayloadLength( byte[] packet, int packetOffset ) {
		return ByteUtil.decodeUInt16( packet, packetOffset + 4 );
	}
	
	public static int getValidatedIp6PayloadLength( byte[] packet, int packetOffset, int packetSize ) {
		int len = getIp6PayloadLength(packet, packetOffset);
		if( len < 0 ) throw new IndexOutOfBoundsException("Packet's payload length is < 0: "+len);
		if( len + 40 > packetSize ) throw new IndexOutOfBoundsException("Packet's payload length is too large for packet of size "+packetSize+": "+len);
		return len;
	}
	
	public static int getIpVersion( byte[] packet, int packetOffset, int packetSize ) {
		return (packet[packetOffset] >> 4) & 0xF;
	}
	
	public static int getIp6TrafficClass( byte[] packet, int packetOffset ) {
		return (ByteUtil.decodeInt32( packet, packetOffset+0) >> 20) & 0xFF;
	}
	
	public static int getIp6ProtocolNumber( byte[] packet, int packetOffset ) {
		return packet[packetOffset+6] & 0xFF;
	}
	
	public static int getIp6PayloadOffset( int packetOffset ) {
		return packetOffset + 40;
	}
	
	public static long calculateIcmp6Checksum( byte[] packet, int offset, int size ) {
		int payloadLength = getValidatedIp6PayloadLength( packet, offset, size );
		
		byte[] data = new byte[40+payloadLength];
		ByteUtil.copy( packet, offset+8,  data,  0, 16 ); // Source address
		ByteUtil.copy( packet, offset+24, data, 16, 16 ); // Destination address
		ByteUtil.encodeInt32(             payloadLength, data, 32 );
		ByteUtil.encodeInt32( getIp6ProtocolNumber( packet, offset ), data, 36 );
		ByteUtil.copy( packet, getIp6PayloadOffset(offset), data, 40, payloadLength );
		return InternetChecksum.checksum( data );
	}
	
	protected static void dumpIcmp6Data( byte[] icmpMessage, int offset, int size, PrintStream ps ) {
		ByteUtil.ensureRoom( size, 0, ICMP_HEADER_SIZE, "ICMP header" );
		
		ps.println( "    ICMP message type: "+(icmpMessage[offset]&0xFF) );
		ps.println( "    ICMP code: "+(icmpMessage[offset+1]&0xFF) );
		ps.println( "    ICMP checksum: "+ByteUtil.decodeUInt16( icmpMessage, offset+2 ) );
	}
	
	protected static void dumpIp6Packet( byte[] packet, int offset, int size, PrintStream ps ) {
		ByteUtil.ensureRoom( size, 0, IP6_HEADER_SIZE, "IP6 header" );
		
		ps.println("IPv6 packet");
		ps.println("  from: "+AddressUtil.formatIp6Address(packet, 8));
		ps.println("  to:   "+AddressUtil.formatIp6Address(packet, 24));
		ps.println("  hoplimit: "+(packet[offset+7] & 0xFF));
		ps.println("  payload length: " + ByteUtil.decodeUInt16(packet, offset+4) );
		ps.println("  traffic class: " + getIp6TrafficClass(packet, offset) );
		ps.println("  protocol number: " + getIp6ProtocolNumber(packet, offset) );
		
		switch( getIp6ProtocolNumber(packet, offset) ) {
		case( 58 ):
			dumpIcmp6Data( packet, getIp6PayloadOffset(offset), getValidatedIp6PayloadLength(packet,offset,size), ps );
		}
		
		ps.println( "    calculated ICMPv6 checksum: "+calculateIcmp6Checksum(packet, offset, size) );
	}
	
	public static void dumpPacket( byte[] packet, int offset, int length, PrintStream ps ) {
		switch( (packet[offset+0] >> 4) & 0xF ) {
		case( 6 ): dumpIp6Packet( packet, offset, length, ps ); 
		}
	}
}
