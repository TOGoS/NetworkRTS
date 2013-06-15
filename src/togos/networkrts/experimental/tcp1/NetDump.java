package togos.networkrts.experimental.tcp1;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class NetDump
{
	public static void main(String[] args) throws Exception {
		DatagramSocket sock = new DatagramSocket(55667);
		
		PacketHandler ph = new EthernetFrameHandler() {
			@Override
			protected void handlePacket(
				long destMac, long sourceMac,
				int protocol, byte[] data, int payloadOffset,
				int payloadLength
			) {
				System.err.printf("Frame to=0x%06X from=0x%06X prot=0x%04X len=%d",
					destMac, sourceMac, protocol, payloadLength
				);
			}
		};
		
		
		DatagramPacket dp = new DatagramPacket(new byte[2048], 2048);
		while( true ) {
			sock.receive(dp);
			ph.handlePacket(dp.getData(), dp.getOffset(), dp.getLength());
		}
	}
}
