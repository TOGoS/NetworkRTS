package togos.networkrts.experimental.netsim1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

import togos.blob.ByteChunk;
import togos.blob.SimpleByteChunk;
import togos.blob.util.BlobUtil;
import togos.networkrts.inet.PacketUtil;
import togos.networkrts.util.ByteUtil;

/**
 * Demonstrates parsing IPv6 headers and ICMP echo requests and responding to
 * them. IP packets must be encapsulated in UDP packets and sent to the port
 * that ICMPResponder listens on, e.g. by TUN2UDP -tun -no-pi
 */
public class ICMPResponder
{
	interface PacketIO {
		public ByteChunk recv();
		public void send(ByteChunk c);
	}
	
	static class DatagramPacketIO implements PacketIO {
		public static final int DEFAULT_MTU = 2048;
		
		DatagramSocket sock;
		int mtu;
		SocketAddress lastReceivedFrom;
		
		public DatagramPacketIO( DatagramSocket sock, int mtu ) {
			this.sock = sock;
			this.mtu = mtu;
		}
		public DatagramPacketIO( DatagramSocket sock ) {
			this( sock, DEFAULT_MTU );
		}
		
		public ByteChunk recv() {
			DatagramPacket p = new DatagramPacket(new byte[mtu], mtu);
			try {
				sock.receive(p);
				lastReceivedFrom = p.getSocketAddress();
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
			return new SimpleByteChunk(p.getData(), p.getOffset(), p.getLength());
		}
		
		public void send(ByteChunk c) {
			// If we don't know where to send it, just drop it.
			if( lastReceivedFrom == null ) return;
			
			DatagramPacket p = new DatagramPacket(c.getBuffer(), c.getOffset(), BlobUtil.toInt(c.getSize()));
			p.setSocketAddress(lastReceivedFrom);
			try {
				sock.send(p);
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		}
	}
	
	static class IPPacketIOWrapper implements PacketIO {
		PacketIO pio;
		
		public IPPacketIOWrapper( PacketIO pio ) {
			this.pio = pio;
		}
		
		public ByteChunk recv() {
			while( true ) {
				ByteChunk c = pio.recv();
				
				System.err.print( "Received packet: " );
				PacketUtil.dumpPacket(c, System.err);
				
				if( c != null ) return c;
			}
		}
		
		public void send(ByteChunk c) {
			System.err.print( "Sending packet: " );
			PacketUtil.dumpPacket(c, System.err);
			pio.send(c);
		}
	}
	
	static class IPPacketHandler {
		protected final PacketIO responder;
		public IPPacketHandler( PacketIO responder ) {
			this.responder = responder;
		}
		
		protected ByteChunk createIcmpEchoResponse( byte[] ping, int offset, int size ) {
			byte[] pong = new byte[size];
			
			ByteUtil.copy( ping, offset, pong, 0, size );

			int trafficClass = 0;
			int flowLabel = 0;
			ByteUtil.encodeInt32( (6 << 28) | (trafficClass << 20) | flowLabel, pong, 0 );
			ByteUtil.copy( ping, offset+8, pong, offset+24, 16 ); // Source address
			ByteUtil.copy( ping, offset+24, pong, offset+8, 16 ); // Dest address
			final int payloadOffset = PacketUtil.IP6_HEADER_SIZE;
			pong[payloadOffset + 0] = (byte)129; // ICMPv6 echo response 
			pong[payloadOffset + 1] =         0; // ICMPv6 code
			pong[payloadOffset + 2] =         0; // Cleared checksum
			pong[payloadOffset + 3] =         0; // Cleared checksum
			// Then calculate actual checksum
			int checksum = (int)PacketUtil.calculateIcmp6Checksum(pong, offset, size);
			ByteUtil.encodeInt16( checksum, pong, payloadOffset + 2 );
			return new SimpleByteChunk(pong);
		}
		
		protected void handleIcmp6Packet( byte[] packet, int offset, int size ) {
			switch( packet[offset+PacketUtil.IP6_HEADER_SIZE] & 0xFF ) {
			case( 128 ): // Echo request
				responder.send( createIcmpEchoResponse(packet, offset, size) );
				break;
			}
		}
		
		public void handleIp6( byte[] packet, int offset, int size ) {
			switch( PacketUtil.getIp6ProtocolNumber(packet, offset) ) {
			case( 58 ): // ICMPv6
				handleIcmp6Packet( packet, offset, size ); break;
			}
		}
		
		public void handle( byte[] packet, int offset, int size ) {
			if( size < PacketUtil.IP4_HEADER_SIZE ) {
				throw new IndexOutOfBoundsException("Packet smaller than IP4 header!");
			}
			switch( (packet[offset] >> 4) & 0xF ) {
			case( 4 ): break;
			case( 6 ): handleIp6( packet, offset, size ); break;
			}
		}
		
		public void handle( ByteChunk bc ) {
			handle( bc.getBuffer(), bc.getOffset(), BlobUtil.toInt(bc.getSize()) );
		}
	}
	
	public static void main( String[] args ) throws Exception {
		DatagramSocket s = new DatagramSocket(7777);
		DatagramPacketIO io = new DatagramPacketIO(s);
		IPPacketIOWrapper ipio = new IPPacketIOWrapper(io);
		IPPacketHandler h = new IPPacketHandler(ipio);
		ByteChunk packet;
		while( (packet = ipio.recv()) != null ) {
			h.handle(packet);
		}
	}
}
