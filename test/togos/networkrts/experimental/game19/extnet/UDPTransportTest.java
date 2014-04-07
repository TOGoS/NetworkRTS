package togos.networkrts.experimental.game19.extnet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

import junit.framework.TestCase;

import togos.networkrts.experimental.game19.util.MessageSender;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.IDGenerator;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.packet19.MalformedDataException;
import togos.networkrts.experimental.packet19.PacketPayloadCodec;

public class UDPTransportTest extends TestCase
{
	// UDPTransport A - DatagramSocket A - Internet DatagramSocket B - UDPTransport B
	
	UDPTransport<String> transportA, transportB;
	static int portA = 13766, portB = 13767;
	DatagramSocket socketA, socketB;
	
	static final Charset UTF8;
	static final CharsetEncoder UTF8_ENCODER;
	static final CharsetDecoder UTF8_DECODER;
	static {
		UTF8 = Charset.forName("utf-8");
		UTF8_ENCODER = UTF8.newEncoder();
		UTF8_ENCODER.onMalformedInput(CodingErrorAction.REPORT);
		UTF8_DECODER = UTF8.newDecoder();
		UTF8_DECODER.onMalformedInput(CodingErrorAction.REPORT);
	}
	IDGenerator idGen;
	PacketPayloadCodec<String> codec = new PacketPayloadCodec<String>() {
		public String decode(byte[] data, int offset, int length) throws MalformedDataException {
			try {
				return new String(data, offset, length, "utf-8");
			} catch( UnsupportedEncodingException e ) {
				throw new MalformedDataException("unsupported encoedign!", e);
			}
		}
		@Override public void encode(String str, OutputStream os) throws IOException {
			os.write( str.getBytes("utf-8") );
		}
	};
	
	Message messageA, messageB; 
	
	MessageSender senderA = new MessageSender() {
		@Override public void sendMessage(Message m) {
			messageA = m;
		}
	};
	MessageSender senderB = new MessageSender() {
		@Override public void sendMessage(Message m) {
			messageB = m;
		}
	};
	
	protected long messageDestBaA, messageDestBaB;
	
	@Override public void setUp() throws SocketException {
		idGen = new IDGenerator(BitAddresses.TYPE_EXTERNAL);
		socketA = new DatagramSocket(portA);
		socketB = new DatagramSocket(portB);
		messageA = messageB = null;
		transportA = new UDPTransport<String>("A", idGen.newId(), socketA, codec, senderA, (messageDestBaA = idGen.newId()));
		transportB = new UDPTransport<String>("A", idGen.newId(), socketB, codec, senderB, (messageDestBaB = idGen.newId()));
		transportA.inetDestSocketAddress = new InetSocketAddress("127.0.0.1", portB);
		transportA.start();
		transportB.start();
	}
	
	@Override protected void tearDown() throws Exception {
		portA += 2;
		portB += 2;
	}
	
	public void testSendMisaddressedMessageToA() throws InterruptedException {
		transportA.sendMessage(Message.create(transportA.transportBitAddress-1, MessageType.INCOMING_PACKET, "Hello"));
		Thread.sleep(100);
		assertNull(transportB.inetDestSocketAddress);
		assertNull(messageB);
	}
	
	public void testMessageToA() throws InterruptedException {
		transportA.sendMessage(Message.create(transportA.transportBitAddress, MessageType.INCOMING_PACKET, "Hello"));
		Thread.sleep(100);
		assertNotNull(messageB);
		assertNotNull(transportB.inetDestSocketAddress);
		assertEquals("Hello", messageB.payload);
		assertEquals(BitAddresses.withMinFlags(messageDestBaB), messageB.minBitAddress);
		assertEquals(BitAddresses.withMaxFlags(messageDestBaB), messageB.maxBitAddress);
		
		// At this point B should be able to respond:
		
		transportB.sendMessage(Message.create(transportB.transportBitAddress, MessageType.INCOMING_PACKET, "Hiya"));
		Thread.sleep(100);
		assertNotNull(messageA);
		assertNotNull(transportA.inetDestSocketAddress);
		assertEquals("Hiya", messageA.payload);
		assertEquals(BitAddresses.withMinFlags(messageDestBaA), messageA.minBitAddress);
		assertEquals(BitAddresses.withMaxFlags(messageDestBaA), messageA.maxBitAddress);
	}
}
