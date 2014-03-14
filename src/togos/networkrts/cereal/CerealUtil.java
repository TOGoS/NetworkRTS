package togos.networkrts.cereal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bitpedia.util.Base32;

public class CerealUtil
{
	static final Pattern BITPRINT_PATTERN = Pattern.compile("urn:bitprint:([A-Z0-9]{32})\\.([A-Z0-9]{39})");
	static final Pattern SHA1_PATTERN = Pattern.compile("urn:sha1:([A-Z0-9]{32})");
	
	public static final byte[] TBB_HEADER = new byte[] { 'T', 'B', 'B', (byte)0x81 };
	public static final String CEREAL_SCHEMA_BITPRINT_URN = "urn:bitprint:HQ25ZRMI7O3UTJWTUYLUH4WXNKMMPSHL.EGNY7A2X5UHQAEYUR5HHARKB6RM235TDU5FA32I";
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
		throw new InvalidEncoding("Unrecognized SHA-1 URN: '"+urn+"'");
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
	
	public static void writeOpImport( byte dest, byte[] opRef, OutputStream os ) throws IOException {
		assert opRef.length == 20;
		
		os.write((byte)0x41);
		os.write(dest);
		os.write(opRef);
	}
	
	public static void writeOpImport( byte dest, String opUrn, OutputStream os ) throws InvalidEncoding, IOException {
		byte[] opRef = extractSha1FromUrn(opUrn);
		writeOpImport( dest, opRef, os );
	}
}
