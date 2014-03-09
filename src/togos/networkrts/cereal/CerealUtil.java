package togos.networkrts.cereal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bitpedia.util.Base32;

public class CerealUtil
{
	static final Pattern BITPRINT_PATTERN = Pattern.compile("urn:bitprint:([A-Z0-9]{32})\\.([A-Z0-9]{39})");
	static final Pattern SHA1_PATTERN = Pattern.compile("sha1:([A-Z0-9]{32})");
	
	public static final byte[] TBB_HEADER = new byte[] { 'T', 'B', 'B', (byte)0x81 };
	public static final String CEREAL_SCHEMA_BITPRINT_URN = "urn:bitprint:5MARGFSZ37CFHHV7TVQRG4ESAE6BWYCM.NZOGMQZEJMCPXKTIB23273MU6Q57JHKXCKBOB3I";
	public static final String CEREAL_SCHEMA_SHA1_URN;
	public static final byte[] CEREAL_SCHEMA_REF;
	static {
		byte[] r;
		String sha1Urn;
		try {
			r = extractSha1FromUrn(CEREAL_SCHEMA_BITPRINT_URN);
			sha1Urn = sha1Urn(CEREAL_SCHEMA_BITPRINT_URN);
		} catch( InvalidEncoding e ) {
			throw new RuntimeException(e);
		} 
		CEREAL_SCHEMA_REF = r;
		CEREAL_SCHEMA_SHA1_URN = sha1Urn;
	}
	
	public static String sha1Urn( byte[] sha1 ) {
		assert sha1 != null;
		assert sha1.length == 20;
		return "urn:sha1:"+Base32.encode(sha1);
	}
	
	public static String sha1Urn( String urn ) throws InvalidEncoding {
		Matcher m;
		if( (m = SHA1_PATTERN.matcher(urn)).matches() ) {
			return urn;
		}
		if( (m = BITPRINT_PATTERN.matcher(urn)).matches() ) {
			return "urn:sha1:"+m.group(1);
		}
		throw new InvalidEncoding("Unrecognized SHA-1 URN: "+urn);
	}
	
	public static final byte[] extractSha1FromUrn( String urn ) throws InvalidEncoding {
		Matcher m;
		if( (m = SHA1_PATTERN.matcher(urn)).matches() ) {
			return Base32.decode( m.group(1) );
		}
		if( (m = BITPRINT_PATTERN.matcher(urn)).matches() ) {
			return Base32.decode( m.group(1) );
		}
		throw new InvalidEncoding("Unrecognized SHA-1 URN: "+urn);
	}
	
	public static short readInt16( byte[] data, int offset ) {
		return (short)(
			((data[offset+0]&0xFF) << 8) |
			((data[offset+1]&0xFF) << 0)
		);
	}
	public static int readInt32( byte[] data, int offset ) {
		return
			((data[offset+0]&0xFF) << 24) |
			((data[offset+1]&0xFF) << 16) |
			((data[offset+2]&0xFF) <<  8) |
			((data[offset+3]&0xFF) <<  0);
	}
	public static long readInt64( byte[] data, int offset ) {
		return
			((long)(data[offset+0]&0xFF) << 56) |
			((long)(data[offset+1]&0xFF) << 48) |
			((long)(data[offset+2]&0xFF) << 40) |
			((long)(data[offset+3]&0xFF) << 32) |
			((long)(data[offset+4]&0xFF) << 24) |
			((long)(data[offset+5]&0xFF) << 16) |
			((long)(data[offset+6]&0xFF) <<  8) |
			((long)(data[offset+7]&0xFF) <<  0);
	}
	
	public static byte[] extract( byte[] a, int offset, int length ) throws InvalidEncoding {
		if( a.length < offset+length ) throw new InvalidEncoding("Data ends prematurely");
		
		byte[] dest = new byte[length];
		for( int i=0, j=offset; i<length; ++i, ++j ) dest[i] = a[j];
		return dest;
	}
	
	public static void writeTbbHeader( byte[] schemaRef, OutputStream os ) throws IOException {
		assert schemaRef.length == 20;
		
		os.write( TBB_HEADER );
		os.write( schemaRef );
	}
	
	public static void writeLibImport( byte[] libRef, OutputStream os ) throws IOException {
		assert libRef.length == 20;
		
		os.write((byte)0x01);
		os.write(libRef);
	}

	public static void writeInt16( long v, OutputStream os ) throws IOException {
		byte[] buf = new byte[2];
		buf[0] = (byte)(v>> 8);
		buf[1] = (byte)(v>> 0);
		os.write(buf);
	}
	public static void writeInt32( long v, OutputStream os ) throws IOException {
		byte[] buf = new byte[4];
		buf[0] = (byte)(v>>24);
		buf[1] = (byte)(v>>16);
		buf[2] = (byte)(v>> 8);
		buf[3] = (byte)(v>> 0);
		os.write(buf);
	}
	public static void writeInt64( long v, OutputStream os ) throws IOException {
		byte[] buf = new byte[8];
		buf[0] = (byte)(v>>56);
		buf[1] = (byte)(v>>48);
		buf[2] = (byte)(v>>40);
		buf[3] = (byte)(v>>32);
		buf[4] = (byte)(v>>24);
		buf[5] = (byte)(v>>16);
		buf[6] = (byte)(v>> 8);
		buf[7] = (byte)(v>> 0);
		os.write(buf);
	}
}
