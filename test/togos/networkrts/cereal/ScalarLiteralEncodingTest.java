package togos.networkrts.cereal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.util.ResourceNotFound;

public class ScalarLiteralEncodingTest extends TestCase
{
	protected void testOptimalEncoding( Number v, byte optimalEncoding ) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ScalarLiteralOps.writeCompactNumber(v, baos);
			byte[] encoded = baos.toByteArray();
			if( optimalEncoding == ScalarLiteralOps.NE_INT6 ) {
				assertEquals(1, encoded.length);
				assertEquals(0x80, encoded[0] & 0x80);
			} else {
				assertEquals(optimalEncoding, encoded[0]);
			}
			DecodeState ds = new DecodeState(ScalarLiteralOps.INSTANCE.ops);
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
	
	public void testInt6Encoding() {
		testOptimalEncoding( -64, ScalarLiteralOps.NE_INT6 );
		testOptimalEncoding(   0, ScalarLiteralOps.NE_INT6 );
		testOptimalEncoding(  63, ScalarLiteralOps.NE_INT6 );
	}
	public void testInt8Encoding() {
		testOptimalEncoding( Byte.MIN_VALUE, ScalarLiteralOps.NE_INT8 );
		testOptimalEncoding(  -65          , ScalarLiteralOps.NE_INT8 );
		testOptimalEncoding(   64          , ScalarLiteralOps.NE_INT8 );
		testOptimalEncoding( Byte.MAX_VALUE, ScalarLiteralOps.NE_INT8 );
	}
	public void testInt16Encoding() {
		testOptimalEncoding( Short.MIN_VALUE , ScalarLiteralOps.NE_INT16 );
		testOptimalEncoding( Byte.MIN_VALUE-1, ScalarLiteralOps.NE_INT16 );
		testOptimalEncoding( Byte.MAX_VALUE+1, ScalarLiteralOps.NE_INT16);
		testOptimalEncoding( Short.MAX_VALUE , ScalarLiteralOps.NE_INT16 );
	}
	public void testInt32Encoding() {
		testOptimalEncoding( Integer.MIN_VALUE, ScalarLiteralOps.NE_INT32 );
		testOptimalEncoding( Short.MIN_VALUE-1, ScalarLiteralOps.NE_INT32 );
		testOptimalEncoding( Short.MAX_VALUE+1, ScalarLiteralOps.NE_INT32 );
		testOptimalEncoding( Integer.MAX_VALUE, ScalarLiteralOps.NE_INT32 );
	}
	public void testInt64Encoding() {
		testOptimalEncoding( Long.MIN_VALUE     , ScalarLiteralOps.NE_INT64 );
		testOptimalEncoding( (long)Integer.MIN_VALUE-1, ScalarLiteralOps.NE_INT64 );
		testOptimalEncoding( (long)Integer.MAX_VALUE+1, ScalarLiteralOps.NE_INT64 );
		testOptimalEncoding( Long.MAX_VALUE     , ScalarLiteralOps.NE_INT64 );
	}
	public void testFloat16Encoding() {
		// Float16Test goes a bit deeper
		testOptimalEncoding( 0.125, ScalarLiteralOps.NE_FLOAT16 );
		testOptimalEncoding( 0.125, ScalarLiteralOps.NE_FLOAT16 );
		testOptimalEncoding( 19.125, ScalarLiteralOps.NE_FLOAT16 );
		testOptimalEncoding( Math.pow(2,-24), ScalarLiteralOps.NE_FLOAT16 );
		testOptimalEncoding( Math.pow(2,-14), ScalarLiteralOps.NE_FLOAT16 );
	}
	public void testFloat32Encoding() {
		testOptimalEncoding( -Float.MAX_VALUE, ScalarLiteralOps.NE_FLOAT32 );
		testOptimalEncoding( -Float.MIN_VALUE, ScalarLiteralOps.NE_FLOAT32 );
		testOptimalEncoding(   1234.125      , ScalarLiteralOps.NE_FLOAT32 );
		testOptimalEncoding(  Float.MIN_VALUE, ScalarLiteralOps.NE_FLOAT32 );
		testOptimalEncoding(  Float.MAX_VALUE, ScalarLiteralOps.NE_FLOAT32 );
	}
	public void testFloat64Encoding() {
		testOptimalEncoding( -Double.MAX_VALUE, ScalarLiteralOps.NE_FLOAT64 );
		testOptimalEncoding( -Double.MIN_VALUE, ScalarLiteralOps.NE_FLOAT64 );
		testOptimalEncoding(  123456789123.125, ScalarLiteralOps.NE_FLOAT64 );
		testOptimalEncoding(  Double.MIN_VALUE, ScalarLiteralOps.NE_FLOAT64 );
		testOptimalEncoding(  Double.MAX_VALUE, ScalarLiteralOps.NE_FLOAT64 );
	}
}
