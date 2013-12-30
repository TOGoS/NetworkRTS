package togos.networkrts.experimental.inet.transport;

import java.net.SocketAddress;

/**
 * Like a DatagramPacket, but immutable.
 */
public class HLDatagram<Payload>
{
	public final SocketAddress sourceAddress;
	public final SocketAddress destAddress;
	public final Payload payload;
	
	public HLDatagram( SocketAddress sourceAddress, SocketAddress destAddress, Payload payload ) {
		this.sourceAddress = sourceAddress;
		this.destAddress = destAddress;
		this.payload = payload;
	}
}
