package togos.networkrts.experimental.inet.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

public class DatagramSocketNetworkInterface
	implements NetworkInterface<HLDatagram<byte[]>>, Runnable
{
	protected final Set<PacketListener<HLDatagram<byte[]>>> packetListeners = new HashSet<PacketListener<HLDatagram<byte[]>>>();
	protected final DatagramSocket sock;
	protected final int maxPacketSize;
	protected final DatagramPacket outPacket;
	protected final SocketAddress localAddress;
	
	public DatagramSocketNetworkInterface( DatagramSocket sock, int maxPacketSize, SocketAddress localAddress ) {
		this.sock = sock;
		this.maxPacketSize = maxPacketSize;
		this.outPacket = new DatagramPacket(new byte[maxPacketSize], maxPacketSize);
		this.localAddress = localAddress;
	}
	
	@Override public void sendPacket(HLDatagram<byte[]> p) {
		try {
			synchronized(outPacket) {
				outPacket.setSocketAddress(p.destAddress);
				outPacket.setData(p.payload);
				sock.send(outPacket);
			}
		} catch( IOException e ) {
			System.err.println("IOException when sending packet");
			e.printStackTrace();
		}
	}
	
	@Override public void addIncomingPacketListener(PacketListener<HLDatagram<byte[]>> pl) {
		synchronized( packetListeners ) {
			packetListeners.add(pl);
		}
	}
	
	public void run() {
		DatagramPacket pack = new DatagramPacket(new byte[maxPacketSize], maxPacketSize);
		while( true ) {
			try {
				pack.setLength(maxPacketSize);
				sock.receive(pack);
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
			
			synchronized( packetListeners ) {
				if( packetListeners.size() == 0 ) continue; 
			}
			
			byte[] buff = pack.getData();
			byte[] data = new byte[pack.getLength()];
			for( int i=data.length-1; i>=0; --i ) data[i] = buff[i];
			
			HLDatagram<byte[]> incomingPacket = new HLDatagram<byte[]>( pack.getSocketAddress(), localAddress, data );
			
			synchronized( packetListeners ) {
				for( PacketListener<HLDatagram<byte[]>> l : packetListeners ) {
					l.packetReceived(incomingPacket);
				}
			}
		}
	}
}
