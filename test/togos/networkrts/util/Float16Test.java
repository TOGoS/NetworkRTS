package togos.networkrts.util;

import togos.networkrts.util.Float16;
import junit.framework.TestCase;

public class Float16Test extends TestCase
{
	protected void assertIsRepresentableAsFloat16( double num ) {
		int encoded = Float16.floatToShortBits((float)num);
		float decoded = Float16.shortBitsToFloat(encoded);
		assertTrue( num == decoded );
	}
	
	static final double[] okayNumbers = new double[] {
		Math.pow(2,-24), Math.pow(2,-14),
		0.5, 0.25, 0.125,
		
		// Integers!
		0, 1, 2, 3,	    0x07FF,
		0x0800, 0x0802, 0x0FFE,
		0x1000, 0x1004, 0x1FFC,
		0x2000, 0x2008, 0x3FF8,
		0x4000, 0x4010, 0x4FF0,
		0x8000, 0x8020, 0xFFE0,
		
		Double.POSITIVE_INFINITY,
	};
	
	public void testNumbersRepresentable() {
		for( float sign=-1; sign<=+1; sign+=2 ) {
			for( double okay : okayNumbers ) {
				assertIsRepresentableAsFloat16( sign*okay );
			}
		}
	}
	
	public void testNaNRepresentable() {
		assertTrue(Float.isNaN(Float16.shortBitsToFloat(Float16.floatToShortBits(Float.NaN))));
	}
}
