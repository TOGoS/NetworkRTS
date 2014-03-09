package togos.networkrts.cereal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import togos.networkrts.util.MemoryRepo;
import togos.networkrts.util.ResourceNotFound;
import junit.framework.TestCase;

public class CerealDecoderTest extends TestCase
{
	MemoryRepo repo;
	CerealDecoder decoder;
	
	public void setUp() {
		repo = new MemoryRepo();
		decoder = new CerealDecoder(repo);
	}
	
	protected void encodeNumber( double num, OutputStream os ) throws IOException {
		
	}
	
	protected byte[] encodeNumber( double num ) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CerealUtil.writeTbbHeader( CerealUtil.CEREAL_SCHEMA_REF, baos );
			CerealUtil.writeLibImport( ScalarLiteralOps.INSTANCE.sha1, baos );
			ScalarLiteralOps.writeNumber( num, baos );
			return baos.toByteArray();
		} catch( IOException e ) {
			// Won't happen!
			throw new RuntimeException(e);
		}
	}
	
	public void testEmptyIsParseError() throws ResourceNotFound {
		byte[] data;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CerealUtil.writeTbbHeader( CerealUtil.CEREAL_SCHEMA_REF, baos );
			CerealUtil.writeLibImport( ScalarLiteralOps.INSTANCE.sha1, baos );
			data = baos.toByteArray();
		} catch( IOException e ) {
			// Won't happen!
			throw new RuntimeException(e);
		}
		try {
			decoder.decode(data);
			fail("It should've thrown an InvalidEncoding!");
		} catch( InvalidEncoding ie ) {
			// Yay
		}
	}
	
	public void testEncodeDecodeNumber() throws InvalidEncoding, ResourceNotFound {
		byte[] data = encodeNumber(-3.5);
		Object v = decoder.decode(data);
		assertTrue( v instanceof Number );
		assertEquals( Double.valueOf(-3.5), v );
	}
}
