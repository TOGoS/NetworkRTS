package togos.networkrts.experimental.game19.demo;

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
	static class PingableNetDevice extends BaseNetDevice {
		public PingableNetDevice( long bitAddress, long ethAddy, IPAddress ipAddy, MessageSender network ) {
			super( bitAddress, network );
			this.ethernetAddress = ethAddy;
			this.ipAddress = ipAddy;
		}
		
		@Override protected void handleEthernetFrame(PacketWrapping<EthernetFrame> pw) {
			System.err.println("Got ethernet frame!");
			super.handleEthernetFrame(pw);
		}
		
		@Override protected void handleIpPacket(PacketWrapping<IPPacket> pw) {
			System.err.println("Got IP packet!");
			super.handleIpPacket(pw);
		}

	}
	
	public static void main( String[] args ) throws SocketException {
		IDGenerator idGen = new IDGenerator(BitAddresses.TYPE_EXTERNAL+1);
		
		long transport0Id = idGen.newId();
		// TODO: Put a ethernet switch between
		long pingableId = idGen.newId();
		long pingableEthAddy = 0x0000123456789abcl;
		IP6Address pingableIpAddress = new IP6Address(new byte[]{
			0x20, 0x01, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05
		});

		
		Network net = new Network();
		
		DatagramSocket dgSock = new DatagramSocket(11388);
		
		PingableNetDevice pingable = new PingableNetDevice(pingableId, pingableEthAddy, pingableIpAddress, net);
		
		net.addComponent(new UDPTransport<EthernetFrame>("UDP Transport 0", transport0Id, dgSock, EthernetFrame.CODEC, net, pingableId));
		net.addComponent(pingable);
		net.start();
	}
}
