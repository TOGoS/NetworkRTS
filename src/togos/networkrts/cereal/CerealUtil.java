package togos.networkrts.cereal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import togos.networkrts.cereal.op.LoadOpcode;
import togos.networkrts.util.HashUtil;

public class CerealUtil
{
	public static final Pattern BITPRINT_PATTERN = Pattern.compile("urn:bitprint:([A-Z0-9]{32})\\.([A-Z0-9]{39})");
	public static final Pattern SHA1_PATTERN = Pattern.compile("urn:sha1:([A-Z0-9]{32})");
	
	public static final byte[] TBB_HEADER = new byte[] { 'T', 'B', 'B', (byte)0x81 };
	public static final String CEREAL_SCHEMA_BITPRINT_URN = "urn:bitprint:HQ25ZRMI7O3UTJWTUYLUH4WXNKMMPSHL.EGNY7A2X5UHQAEYUR5HHARKB6RM235TDU5FA32I";
	public static final String CEREAL_SCHEMA_SHA1_URN;
	public static final byte[] CEREAL_SCHEMA_REF;
	static {
		byte[] r;
		String sha1Urn;
		try {
			r = HashUtil.extractSha1FromUrn(CEREAL_SCHEMA_BITPRINT_URN);
			sha1Urn = HashUtil.sha1Urn(CEREAL_SCHEMA_BITPRINT_URN);
		} catch( InvalidEncoding e ) {
			throw new RuntimeException(e);
		} 
		CEREAL_SCHEMA_REF = r;
		CEREAL_SCHEMA_SHA1_URN = sha1Urn;
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
	
	public static void writeTbbHeader( OutputStream os ) throws IOException {
		writeTbbHeader(CEREAL_SCHEMA_REF, os);
	}
	
	public static void writeOpImport( byte dest, byte[] opRef, OutputStream os ) throws IOException {
		assert opRef.length == 20;
		
		os.write((byte)0x41);
		os.write(dest);
		os.write(opRef);
	}
	
	public static void writeOpImport( byte dest, String opUrn, OutputStream os ) throws InvalidEncoding, IOException {
		byte[] opRef = HashUtil.extractSha1FromUrn(opUrn);
		writeOpImport( dest, opRef, os );
	}
	
	public static void writeHeaderWithImports( OpcodeDefinition[] imports, OutputStream os ) throws InvalidEncoding, IOException {
		writeTbbHeader(os);
		for( int i=0; i<imports.length && i<256; ++i  ) {
			if( imports[i] == null ) continue;
			if( i == 0x41 && !LoadOpcode.INSTANCE.getUrn().equals(imports[i].getUrn()) ) {
				throw new UnsupportedOperationException("Overwriting op 0x41");
			}
			writeOpImport((byte)i, imports[i].getUrn(), os);
		}
	}
}
