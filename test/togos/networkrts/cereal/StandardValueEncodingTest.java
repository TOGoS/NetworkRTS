package togos.networkrts.cereal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.util.HashUtil;
import togos.networkrts.util.ResourceNotFound;

public class StandardValueEncodingTest extends BaseCerealDecoderTest
{
	protected void testOptimalEncoding( Number v, byte optimalEncoding ) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StandardValueOps.writeNumberCompact(v, baos);
			byte[] encoded = baos.toByteArray();
			if( optimalEncoding == StandardValueOps.NE_INT6 ) {
				assertEquals(1, encoded.length);
				assertEquals(v.intValue(), encoded[0]);
			} else {
				assertEquals(optimalEncoding, encoded[0]);
			}
			DecodeState ds = new DecodeState(StandardValueOps.DEFAULT_OP_TABLE);
			ds.process(encoded, 0, null);
			if( v instanceof Double || v instanceof Float ) {
				assertEquals( v.doubleValue(), ((Number)ds.getValue()).doubleValue() );
			} else {
				assertEquals( v.longValue(), ((Number)ds.getValue()).longValue() );
			}
		} catch( IOException e ) {
			throw new RuntimeException(e);
		} catch( InvalidEncoding e ) {
			throw new RuntimeException(e);
		} catch( ResourceNotFound e ) {
			throw new RuntimeException(e);
		}
	}
	
	//// Numbers!
	
	public void testEncodeDecodeNumber() throws InvalidEncoding, ResourceNotFound {
		byte[] data = encodeStuff(-3.5);
		Object v = decoder.decode(data);
		assertTrue( v instanceof Number );
		assertEqualsish( -3.5, v );
	}
	
	public void testInt6Encoding() {
		testOptimalEncoding( -64, StandardValueOps.NE_INT6 );
		testOptimalEncoding(   0, StandardValueOps.NE_INT6 );
		testOptimalEncoding(  63, StandardValueOps.NE_INT6 );
	}
	public void testInt8Encoding() {
		testOptimalEncoding( Byte.MIN_VALUE, StandardValueOps.NE_INT8 );
		testOptimalEncoding(  -65          , StandardValueOps.NE_INT8 );
		testOptimalEncoding(   64          , StandardValueOps.NE_INT8 );
		testOptimalEncoding( Byte.MAX_VALUE, StandardValueOps.NE_INT8 );
	}
	public void testInt16Encoding() {
		testOptimalEncoding( Short.MIN_VALUE , StandardValueOps.NE_INT16 );
		testOptimalEncoding( Byte.MIN_VALUE-1, StandardValueOps.NE_INT16 );
		testOptimalEncoding( Byte.MAX_VALUE+1, StandardValueOps.NE_INT16 );
		testOptimalEncoding( Short.MAX_VALUE , StandardValueOps.NE_INT16 );
	}
	public void testInt32Encoding() {
		testOptimalEncoding( Integer.MIN_VALUE, StandardValueOps.NE_INT32 );
		testOptimalEncoding( Short.MIN_VALUE-1, StandardValueOps.NE_INT32 );
		testOptimalEncoding( Short.MAX_VALUE+1, StandardValueOps.NE_INT32 );
		testOptimalEncoding( Integer.MAX_VALUE, StandardValueOps.NE_INT32 );
	}
	public void testInt64Encoding() {
		testOptimalEncoding( Long.MIN_VALUE     , StandardValueOps.NE_INT64 );
		testOptimalEncoding( (long)Integer.MIN_VALUE-1, StandardValueOps.NE_INT64 );
		testOptimalEncoding( (long)Integer.MAX_VALUE+1, StandardValueOps.NE_INT64 );
		testOptimalEncoding( Long.MAX_VALUE     , StandardValueOps.NE_INT64 );
	}
	public void testFloat16Encoding() {
		// Float16Test goes a bit deeper
		testOptimalEncoding( 0.125, StandardValueOps.NE_FLOAT16 );
		testOptimalEncoding( 0.125, StandardValueOps.NE_FLOAT16 );
		testOptimalEncoding( 19.125, StandardValueOps.NE_FLOAT16 );
		testOptimalEncoding( Math.pow(2,-24), StandardValueOps.NE_FLOAT16 );
		testOptimalEncoding( Math.pow(2,-14), StandardValueOps.NE_FLOAT16 );
	}
	public void testFloat32Encoding() {
		testOptimalEncoding( -Float.MAX_VALUE, StandardValueOps.NE_FLOAT32 );
		testOptimalEncoding( -Float.MIN_VALUE, StandardValueOps.NE_FLOAT32 );
		testOptimalEncoding(   1234.125      , StandardValueOps.NE_FLOAT32 );
		testOptimalEncoding(  Float.MIN_VALUE, StandardValueOps.NE_FLOAT32 );
		testOptimalEncoding(  Float.MAX_VALUE, StandardValueOps.NE_FLOAT32 );
	}
	public void testFloat64Encoding() {
		testOptimalEncoding( -Double.MAX_VALUE, StandardValueOps.NE_FLOAT64 );
		testOptimalEncoding( -Double.MIN_VALUE, StandardValueOps.NE_FLOAT64 );
		testOptimalEncoding(  123456789123.125, StandardValueOps.NE_FLOAT64 );
		testOptimalEncoding(  Double.MIN_VALUE, StandardValueOps.NE_FLOAT64 );
		testOptimalEncoding(  Double.MAX_VALUE, StandardValueOps.NE_FLOAT64 );
	}
	
	//// Stuff!
	
	public void testEncodeDecodeStuff() throws InvalidEncoding, ResourceNotFound, IOException {
		Object[] things = new Object[] {
			new byte[] { 1, 2, 3, 4, 5, 6, 127, -128 },
			new Long(Long.MAX_VALUE),
			new Double(Double.MAX_VALUE),
			new Double(Double.MIN_VALUE),
			new Float(Float.MIN_VALUE),
			Boolean.TRUE,
			Boolean.FALSE,
			Default.INSTANCE,
			new SHA1ObjectReference(CerealUtil.CEREAL_SCHEMA_REF, true),
			new SHA1ObjectReference(CerealUtil.CEREAL_SCHEMA_REF, false)
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
