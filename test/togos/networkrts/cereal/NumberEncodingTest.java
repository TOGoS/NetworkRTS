package togos.networkrts.cereal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

public class NumberEncodingTest extends TestCase
{
	protected static String debugBase128Bytes( byte[] data ) {
		String s = "";
		for( int i=0; i<data.length; ++i ) {
			if( i>0 ) s += " ";
			s += String.format("%8s", Integer.toBinaryString(data[i]&0xFF)).replace(' ', '0');
		}
		return s;
	}
	
	protected void testUnsignedBase128Encode( long number, byte[] expected ) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			NumberEncoding.writeUnsignedBase128( number, baos );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		byte[] actual = baos.toByteArray();
		assertEquals( debugBase128Bytes(expected), debugBase128Bytes(actual) );
	}
	
	/*
	 
	 * Test vector from https://en.wikipedia.org/wiki/Variable-length_quantity
	 
0x00000000	0x00
0x0000007F	0x7F
0x00000080	0x81 0x00
0x00002000	0xC0 0x00
0x00003FFF	0xFF 0x7F
0x00004000	0x81 0x80 0x00
0x001FFFFF	0xFF 0xFF 0x7F
0x00200000	0x81 0x80 0x80 0x00
0x08000000	0xC0 0x80 0x80 0x00
0x0FFFFFFF	0xFF 0xFF 0xFF 0x7F
	 */
	
	public void testUnsignedBase128Encode() {
		testUnsignedBase128Encode( 0x00000000, new byte[]{ 0x00 } );
		testUnsignedBase128Encode( 0x0000007F, new byte[]{ 0x7F } );
		testUnsignedBase128Encode( 0x00000080, new byte[]{ (byte)0x81, 0x00 } );
		testUnsignedBase128Encode( 0x00001000, new byte[]{ (byte)0xA0, 0x00} );
		testUnsignedBase128Encode( 0x00002000, new byte[]{ (byte)0xC0, 0x00 } );
		testUnsignedBase128Encode( 0x00003FFF, new byte[]{ (byte)0xFF, 0x7F } );
		testUnsignedBase128Encode( 0x00004000, new byte[]{ (byte)0x81, (byte)0x80, 0x00 } );
		testUnsignedBase128Encode( 0x001FFFFF, new byte[]{ (byte)0xFF, (byte)0xFF, 0x7F } );
		testUnsignedBase128Encode( 0x00200000, new byte[]{ (byte)0x81, (byte)0x80, (byte)0x80, 0x00 } );
		testUnsignedBase128Encode( 0x08000000, new byte[]{ (byte)0xC0, (byte)0x80, (byte)0x80, 0x00 } );
		testUnsignedBase128Encode( 0x0FFFFFFF, new byte[]{ (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x7F } );
	}
	
	protected void testUnsignedBase128EncodeDecode( final long v ) throws InvalidEncoding {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final int offset = 5;
		try {
			// Write some padding to ensure things don't only work when offset = 0
			for( int i=0; i<offset; ++i ) baos.write((byte)'a');
			NumberEncoding.writeUnsignedBase128( v, baos );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		byte[] b = baos.toByteArray();
		long decodeResult = NumberEncoding.readUnsignedBase128(b, offset);
		assertEquals( b.length-offset, NumberEncoding.base128Skip(decodeResult) );
		assertEquals( v, NumberEncoding.base128Value(decodeResult) );
	}
	
	public void testUnsignedBase128EncodeDecode() throws InvalidEncoding {
		testUnsignedBase128EncodeDecode( 0x00000000);
		testUnsignedBase128EncodeDecode( 0x0000007F );
		testUnsignedBase128EncodeDecode( 0x00000080 );
		testUnsignedBase128EncodeDecode( 0x00002000 );
		testUnsignedBase128EncodeDecode( 0x00003FFF );
		testUnsignedBase128EncodeDecode( 0x00004000 );
		testUnsignedBase128EncodeDecode( 0x001FFFFF );
		testUnsignedBase128EncodeDecode( 0x00200000 );
		testUnsignedBase128EncodeDecode( 0x08000000 );
		testUnsignedBase128EncodeDecode( 0x0FFFFFFF );
		testUnsignedBase128EncodeDecode( 0x0000000000000000 );
		testUnsignedBase128EncodeDecode( 0x007FABCDEF123456l );
		testUnsignedBase128EncodeDecode( 0x00FFABCDEF123456l );
		try {
			testUnsignedBase128EncodeDecode( 0x01FFABCDEF123456l );
			fail("Should have thrown exception when trying to encode too-large number");
		} catch( UnsupportedOperationException e ) {
		}
	}
	
	public void testUnsignedBase128Decode() {
		byte[] buf = new byte[8];
		for( int i=0; i<8; ++i ) buf[i] = (byte)0x80;
		try {
			NumberEncoding.readUnsignedBase128(buf, 0);
			fail("readUnsignedBase128 should have thrown InvalidEncoding upon finding 8th non-final byte");
		} catch( InvalidEncoding e ) {
		}
	}
}
