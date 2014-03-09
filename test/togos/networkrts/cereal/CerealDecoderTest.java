package togos.networkrts.cereal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;
import togos.networkrts.util.MemoryRepo;
import togos.networkrts.util.ResourceNotFound;

public class CerealDecoderTest extends TestCase
{
	MemoryRepo repo;
	CerealDecoder decoder;
	
	protected static OperationMetaLibrary defaultMetaLibrary = new OperationMetaLibrary();
	static {
		defaultMetaLibrary.addLibrary( ScalarLiteralOps.INSTANCE );
	}
	
	public void setUp() {
		repo = new MemoryRepo();
		decoder = new CerealDecoder(repo, defaultMetaLibrary.getInitialDecodeState());
	}
	
	protected byte[] encodeNumber( double num ) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CerealUtil.writeTbbHeader( CerealUtil.CEREAL_SCHEMA_REF, baos );
			CerealUtil.writeLibImport( ScalarLiteralOps.INSTANCE.sha1, baos );
			ScalarLiteralOps.writeNativeNumber( num, baos );
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
