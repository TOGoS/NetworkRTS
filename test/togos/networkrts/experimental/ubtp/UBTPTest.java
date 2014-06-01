package togos.networkrts.experimental.ubtp;

import java.util.Random;
import java.util.zip.CRC32;

import togos.networkrts.experimental.tcp1.PacketHandler;

import junit.framework.TestCase;

public class UBTPTest extends TestCase
{
	byte[] writeBuffer = new byte[2048];
	int writtenSegOff, writtenSegLen;
	
	byte[] readBuffer = new byte[2048];
	int readSegOff, readSegLen;
	
	PacketHandler blockHandler;
	UBTPPacketHandler packetHandler;
	UBTPPacketWriter uos;
	
	public void setUp() {
		Random r = new Random();
		r.nextBytes(writeBuffer);
		
		blockHandler = new PacketHandler() {
			@Override public void handlePacket( byte[] data, int offset, int length ) {
				for( int i=0; i<length; ++i ) {
					readBuffer[i] = data[offset+i];
				}
				readSegOff = 0;
				readSegLen = length;
			}
		};
		
		packetHandler = new UBTPPacketHandler(blockHandler);
		
		uos = new UBTPPacketWriter(1025, packetHandler);
	}
	
	protected void assertEquals( byte[] a, int ao, int al, byte[] b, int bo, int bl ) {
		assertEquals( al, bl );
		for( int i=0; i<al; ++i ) {
			assertEquals( a[ao+i], b[bo+i] );
		}
	}
	
	protected void assertTxRxMatch( int off, int len ) {
		uos.write( writeBuffer, off, len );
		uos.flush();
		assertEquals( writeBuffer, off, len, readBuffer, readSegOff, readSegLen );
	}
	
	public void testTxRx( int minSize, int maxSize ) {
		assertTxRxMatch( 0, minSize );
		assertTxRxMatch( maxSize-32, 32 );
		assertTxRxMatch( 0, maxSize );
		
		Random r = new Random();
		for( int i=0; i<100; ++i ) {
			int off = r.nextInt(writeBuffer.length);
			int len = r.nextInt(Math.min(maxSize, writeBuffer.length-off));
			assertTxRxMatch( off, len );
		}
	}
	
	public void testTxRxSmallBlocks() {
		testTxRx(0, 1025-3);
	}
	
	public void testTxRxLargeBlocks() {
		testTxRx(1025-3+1, 65535);
	}
}
