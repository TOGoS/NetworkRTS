package togos.networkrts.experimental.game19.demo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import togos.networkrts.experimental.game19.extnet.BaseNetDevice;
import togos.networkrts.experimental.game19.extnet.Network;
import togos.networkrts.experimental.game19.extnet.UDPTransport;
import togos.networkrts.experimental.game19.util.MessageSender;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.IDGenerator;
import togos.networkrts.experimental.packet19.EthernetFrame;
import togos.networkrts.experimental.packet19.IP6Address;
import togos.networkrts.experimental.packet19.IPAddress;
import togos.networkrts.experimental.packet19.IPPacket;
import togos.networkrts.experimental.packet19.PacketWrapping;

public class PingableNetDeviceDemo
{
	public boolean debug = false;
	
	class PingableNetDevice extends BaseNetDevice {
		public PingableNetDevice( long bitAddress, long ethAddy, IPAddress ipAddy, MessageSender network ) {
			super( bitAddress, network );
			this.ethernetAddress = ethAddy;
			this.ipAddress = ipAddy;
		}
		
		@Override protected void handleEthernetFrame(PacketWrapping<EthernetFrame> pw) {
			pw.payload.getPayload();
			if( debug ) System.err.println("Got "+pw.payload);
			super.handleEthernetFrame(pw);
		}
		
		@Override protected void handleIpPacket(PacketWrapping<IPPacket> pw) {
			pw.payload.getPayload();
			if( debug ) System.err.println("Got "+pw.payload);
			super.handleIpPacket(pw);
		}
	}
	
	public void run() throws SocketException {
		IDGenerator idGen = new IDGenerator(BitAddresses.TYPE_EXTERNAL+1);
		
		long transport0Id = idGen.newId();
		// TODO: Put a ethernet switch between
		long pingableId = idGen.newId();
		//long pingableEthAddy = 0x0000563456789abcl;
		long pingableEthAddy = 0x0000F613DE79334Bl;
		IP6Address pingableIpAddress = new IP6Address(new byte[]{
			0x20, 0x01, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06
		});
		
		Network net = new Network();
		
		DatagramSocket dgSock = new DatagramSocket(11388);
		
		PingableNetDevice pingable = new PingableNetDevice(pingableId, pingableEthAddy, pingableIpAddress, net);
		pingable.debug = debug;
		
		net.addComponent(new UDPTransport<EthernetFrame>("UDP Transport 0", transport0Id, dgSock, EthernetFrame.CODEC, net, pingableId) {
			@Override protected void packetReceived(DatagramPacket dgPack) {
				if( debug ) {
					System.err.println("Received packet, length = "+dgPack.getLength());
					byte[] data = dgPack.getData();
					for( int i=0; i<dgPack.getLength(); ++i ) {
						System.err.print(String.format("%02x ", data[i]));
						if( (i+1) % 18 == 0 ) System.err.println();
					}
					System.err.println();
				}
				
				super.packetReceived(dgPack);
			}
		});
		net.addComponent(pingable);
		net.start();
	}
	
	public static void main( String[] args ) throws SocketException {
		PingableNetDeviceDemo pndd = new PingableNetDeviceDemo();
		pndd.run();
	}
}
