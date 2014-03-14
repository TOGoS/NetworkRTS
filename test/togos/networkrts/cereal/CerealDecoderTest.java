package togos.networkrts.cereal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import junit.framework.TestCase;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.util.MemoryRepo;
import togos.networkrts.util.ResourceNotFound;

public class CerealDecoderTest extends TestCase
{
	MemoryRepo repo;
	CerealDecoder decoder;
	
	public void setUp() {
		repo = new MemoryRepo();
		decoder = new CerealDecoder(repo, new DecodeState(Opcodes.createDefaultOpTable()));
		//decoder = new CerealDecoder(repo, new DecodeState(ScalarLiterals.DEFAULT_OP_TABLE));
	}
	
	protected void writeOpDefs( OpcodeDefinition[] opDefs, OutputStream os ) throws InvalidEncoding, IOException {
		for( int i=0; i<256 && i<opDefs.length; ++i ) {
			if( opDefs[i] != null ) {
				CerealUtil.writeOpImport((byte)i, opDefs[i].getUrn(), os);
			}
		}
	}
	
	protected byte[] encodeStuff( Object...stuff ) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CerealUtil.writeTbbHeader( CerealUtil.CEREAL_SCHEMA_REF, baos );
			writeOpDefs( ScalarLiterals.SCALAR_LITERAL_OPS, baos );
			for( Object thing : stuff ) {
				ScalarLiterals.writeValue( thing, baos );
			}
			return baos.toByteArray();
		} catch( InvalidEncoding e ) {
			// Won't happen!
			throw new RuntimeException(e);
		} catch( IOException e ) {
			// Won't happen!
			throw new RuntimeException(e);
		}
	}
	
	public void testEmptyStackIsParseError() throws ResourceNotFound {
		byte[] data;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CerealUtil.writeTbbHeader( CerealUtil.CEREAL_SCHEMA_REF, baos );
			writeOpDefs( ScalarLiterals.SCALAR_LITERAL_OPS, baos );
			data = baos.toByteArray();
		} catch( InvalidEncoding e ) {
			// Won't happen!
			throw new RuntimeException(e);
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
	
	protected void assertEqualsish( Object a, Object b ) {
		if( a instanceof byte[] && b instanceof byte[] ) {
			byte[] baa = (byte[])a, bab = (byte[])b;
			assertEquals( baa.length, bab.length );
			for( int i=0; i<baa.length; ++i ) assertEquals(baa[i], bab[i]);
		} else if( a instanceof Number && b instanceof Number ) {
			assertTrue( (((Number)a).doubleValue() == ((Number)b).doubleValue()) );
			assertEquals( ((Number)a).longValue(), ((Number)b).longValue() ); 
		} else {
			assertEquals(a, b);
		}
	}
	
	public void testEncodeDecodeNumber() throws InvalidEncoding, ResourceNotFound {
		byte[] data = encodeStuff(-3.5);
		Object v = decoder.decode(data);
		assertTrue( v instanceof Number );
		assertEqualsish( -3.5, v );
	}
	
	public void testEncodeDecodeStuff() throws InvalidEncoding, ResourceNotFound, IOException {
		Object[] things = new Object[] {
			new byte[] { 1, 2, 3, 4, 5, 6, 127, -128 },
			new Long(Long.MAX_VALUE),
			new Double(Double.MAX_VALUE),
			new Double(Double.MIN_VALUE),
			new Float(Float.MIN_VALUE)
		};
		
		byte[] data = encodeStuff( things );
		DecodeState ds = decoder.decodeToDecodeState(data);
		Object[] stack = ds.getStackSnapshot();
		assertEquals( things.length, stack.length );
		for( int i=0; i<things.length; ++i ) {
			assertEqualsish( things[i], stack[i] );
		}
	}
}
