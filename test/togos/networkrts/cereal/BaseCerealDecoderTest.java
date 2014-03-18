package togos.networkrts.cereal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.util.MemoryRepo;

public abstract class BaseCerealDecoderTest extends TestCase
{
	MemoryRepo repo;
	CerealDecoder decoder;
	
	public void setUp() {
		repo = new MemoryRepo();
		decoder = new CerealDecoder(repo, new DecodeState(Opcodes.createDefaultOpTable()));
		//decoder = new CerealDecoder(repo, new DecodeState(ScalarLiterals.DEFAULT_OP_TABLE));
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
	
	protected byte[] encodeStuff( Object...stuff ) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CerealUtil.writeHeaderWithImports( StandardValueOps.STANDARD_OPS, baos );
			for( Object thing : stuff ) {
				StandardValueOps.writeValue( thing, baos );
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
}
