package togos.networkrts.experimental.game19.demo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

import togos.networkrts.experimental.game19.extnet.NetworkComponent;
import togos.networkrts.experimental.game19.util.MessageSender;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.packet19.MalformedDataException;
import togos.networkrts.experimental.packet19.PacketPayloadCodec;
import togos.networkrts.util.BitAddressUtil;

public class UDPTransport extends Thread implements NetworkComponent
{
	// IP network I <-> UDPTransport <-> Message network M
	
	protected final DatagramSocket datagramSocket;
	protected final boolean autoInetAddress; // The latest address we've received from is where we'll send to
	protected SocketAddress inetDestSocketAddress;
	protected final PacketPayloadCodec<?> codec;
	protected final long transportBitAddress, messageDestBitAddress;
	protected final MessageSender messageSender;
	
	public UDPTransport( String name, long tBa, DatagramSocket dgSock, PacketPayloadCodec<Message> codec, MessageSender messageSender, long mdBa ) {
		super(name);
		this.transportBitAddress = tBa;
		this.datagramSocket = dgSock;
		this.autoInetAddress = true;
		this.codec = codec;
		this.messageSender = messageSender;
		this.messageDestBitAddress = mdBa;
	}
	
	public void _run() throws IOException {
		byte[] buffer = new byte[2048];
		DatagramPacket dgPack = new DatagramPacket(buffer, buffer.length);
		while( !interrupted() ) {
			dgPack.setData(buffer);
			datagramSocket.receive(dgPack);
			if( autoInetAddress ) {
				inetDestSocketAddress = dgPack.getSocketAddress();
			}
			try {
				Object data = codec.decode(buffer, 0, dgPack.getLength());
				Message m = Message.create(messageDestBitAddress, MessageType.INCOMING_PACKET, data);
				messageSender.sendMessage(m);
			} catch( MalformedDataException e ) {
				System.err.println("Received malformed data un UDP packet");
				e.printStackTrace();
			}
		}
	}
	
	public void run() {
		try {
			_run();
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}

	@Override public void sendMessage( Message m ) {
		if( !BitAddressUtil.rangeContains(m, transportBitAddress) ) return;
		
		// TODO: Encode and send
	}
	
	@Override public void halt() {
		// TODO: Might need to close the socket...
		interrupt();
	}
}
