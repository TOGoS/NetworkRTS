package togos.networkrts.resource;

import togos.blob.ByteChunk;

public class SerializationUtil
{
	static byte[] TBB_HEADER = {'T','B','B',(byte)0x81}; 

	/*
	public static void writeTbbHeader( OutputStream s, byte[] schemaHash ) throws IOException {
		s.write( TBB_HEADER );
		writeHash( s, schemaHash );
	}
	public static void writeTbbHeader( OutputStream s, ByteChunk schemaHash ) throws IOException {
		s.write( TBB_HEADER );
		writeHash( s, schemaHash );
	}
	
	public static void writeHash( OutputStream s, byte[] c ) throws IOException {
		if( c.length != 20 ) {
			throw new RuntimeException("Hash size should be 20, but is "+c.length);
		}
		s.write( c );
	}
	public static void writeHash( OutputStream s, ByteChunk c ) throws IOException {
		if( c.getSize() != 20 ) {
			throw new RuntimeException("Hash size should be 20, but is "+c.getSize());
		}
		s.write( c.getBuffer(), c.getOffset(), c.getSize() );
	}
	
	public static void writeExact( OutputStream s, byte[] data, int length ) throws IOException {
		if( data.length != length ) {
			throw new RuntimeException("Expected to write exactly "+length+" bytes, byt array given has length: "+data.length);
		}
		s.write(data);
	}
	*/

	public static void copy( byte[] destBuf, int destOffset, byte[] srcBuf, int srcOffset, int length ) {
		for( int i=0; i<length; ++i ) {
			destBuf[destOffset++] = srcBuf[srcOffset++];
		}
	}
	
	public static void writeTbbHeader( byte[] dest, byte[] schemaHash ) {
		copy( dest, 0, TBB_HEADER, 0,  4 );
		copy( dest, 4, schemaHash, 0, 20 );
	}
	
	public static void copyHash( byte[] destBuf, int destOffset, ByteChunk hash ) {
		if( hash.getSize() != 20 ) {
			throw new RuntimeException("Who ever heard of a hash whose length wasn't 20?");
		}
		copy( destBuf, destOffset, hash.getBuffer(), hash.getOffset(), 20 );
	}
	
	public static byte[] copyOf( byte[] buf, int offset, int length ) {
		byte[] dest = new byte[length];
		copy( dest, 0, buf, offset, length );
		return dest;
	}
	
	/*
	public static void writeShortString( OutputStream s, ByteChunk c ) throws IOException {
		if( c.getSize() > 255 ) {
			throw new RuntimeException("Can't write ByteChunk as shortstring because it's too long: "+c.getSize());
		}
		s.write((byte)c.getSize());
		s.write(c.getBuffer(), c.getOffset(), c.getSize());
	}
	*/
}
