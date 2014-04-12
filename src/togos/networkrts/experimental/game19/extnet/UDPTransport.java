package togos.networkrts.experimental.game19.extnet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

import togos.networkrts.experimental.game19.util.MessageSender;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.packet19.MalformedDataException;
import togos.networkrts.experimental.packet19.PacketPayloadCodec;
import togos.networkrts.util.BitAddressUtil;

/**
 * Bridge between X-over-UDP and X-over-Message networks
 * X usually = Ethernet, but 'codec' makes the decision. 
 */
public class UDPTransport<F> extends Thread implements NetworkComponent
{
	// IP network I <-> UDPTransport <-> Message network M
	
	protected final DatagramSocket datagramSocket;
	protected final DatagramPacket outgoingPacket = new DatagramPacket(new byte[0], 0);
	protected final boolean autoInetAddress; // The latest address we've received from is where we'll send to
	protected SocketAddress inetDestSocketAddress;
	protected final PacketPayloadCodec<F> codec;
	protected final long transportBitAddress, messageDestBitAddress;
	protected final MessageSender messageSender;
	
	public UDPTransport( String name, long tBa, DatagramSocket dgSock, PacketPayloadCodec<F> codec, MessageSender messageSender, long mdBa ) {
		super(name);
		this.transportBitAddress = tBa;
		this.datagramSocket = dgSock;
		this.autoInetAddress = true;
		this.codec = codec;
		this.messageSender = messageSender;
		this.messageDestBitAddress = mdBa;
	}
	
	protected void packetReceived( DatagramPacket dgPack ) {
		if( autoInetAddress ) {
			inetDestSocketAddress = dgPack.getSocketAddress();
		}
		try {
			Object data = codec.decode(dgPack.getData(), 0, dgPack.getLength());
			Message m = Message.create(messageDestBitAddress, MessageType.INCOMING_PACKET, transportBitAddress, data);
			messageSender.sendMessage(m);
		} catch( MalformedDataException e ) {
			System.err.println("Received malformed data in UDP packet");
			e.printStackTrace();
		} catch( Exception e ) {
			System.err.println("Exception while handling UDP packet");
			e.printStackTrace();
		}
	}
	
	public void _run() throws IOException {
		byte[] buffer = new byte[2048];
		DatagramPacket dgPack = new DatagramPacket(buffer, buffer.length);
		while( !interrupted() ) {
			dgPack.setData(buffer);
			datagramSocket.receive(dgPack);
			packetReceived( dgPack );
		}
	}
	
	public void run() {
		try {
			_run();
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
	
	long postErrorSendSuppressionTime = 5*1000;
	long lastSendErrorTime = System.currentTimeMillis() - postErrorSendSuppressionTime*2;
	
	@Override public void sendMessage( Message m ) {
		if( !BitAddressUtil.rangeContains(m, transportBitAddress) ) return;
		
		if( System.currentTimeMillis() - lastSendErrorTime < postErrorSendSuppressionTime ) return;
		
		byte[] encoded;
		try {
			@SuppressWarnings("unchecked")
			F f = (F)m.payload;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			codec.encode(f, baos);
			encoded = baos.toByteArray();
		} catch( IOException e ) {
			// This should never happen.
			throw new RuntimeException(e);
		}
		
		synchronized( outgoingPacket ) {
			outgoingPacket.setData(encoded);
			outgoingPacket.setSocketAddress(inetDestSocketAddress);
			try {
				datagramSocket.send(outgoingPacket);
			} catch( IOException e ) {
				lastSendErrorTime = System.currentTimeMillis();
				System.err.println("Error sending UDP packet");
				System.err.println("Will not attempt to send for "+postErrorSendSuppressionTime/1000.0+" seconds");
				e.printStackTrace();
			}
		}
	}
	
	@Override public void halt() {
		// TODO: Might need to close the socket.
		interrupt();
	}
}
