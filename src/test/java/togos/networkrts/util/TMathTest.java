package togos.networkrts.util;

import junit.framework.TestCase;

public class TMathTest extends TestCase
{
	public void testPeriodic24_32() {
		assertEquals(             0, TMath.periodic24_32(0x000000));
		assertEquals( 32767 * 65536, TMath.periodic24_32(0x400000));
		assertEquals(             0, TMath.periodic24_32(0x800000));
		assertEquals(-32767 * 65536, TMath.periodic24_32(0xC00000));
		assertTrue( TMath.periodic24_32(0x000000) < TMath.periodic24_32(0x001000) );
		assertTrue( TMath.periodic24_32(0x001000) < TMath.periodic24_32(0x002000) );
		assertTrue( TMath.periodic24_32(0x002000) < TMath.periodic24_32(0x010000) );
		assertTrue( TMath.periodic24_32(0x010000) < TMath.periodic24_32(0x020000) );
		assertTrue( TMath.periodic24_32(0x002000) < TMath.periodic24_32(0x400000) );
		assertTrue( TMath.periodic24_32(0x400000) > TMath.periodic24_32(0x410000) );
		assertTrue( TMath.periodic24_32(0x410000) > TMath.periodic24_32(0x411000) );
	}
	
	public void testPeriodic24() {
		assertEquals( 0.0f, TMath.periodic24(0x000000));
		assertEquals( 1.0f, TMath.periodic24(0x400000));
		assertEquals( 0.0f, TMath.periodic24(0x800000));
		assertEquals(-1.0f, TMath.periodic24(0xC00000));
		assertTrue( TMath.periodic24(0x000000) < TMath.periodic24(0x001000) );
		assertTrue( TMath.periodic24(0x001000) < TMath.periodic24(0x002000) );
		assertTrue( TMath.periodic24(0x002000) < TMath.periodic24(0x010000) );
		assertTrue( TMath.periodic24(0x010000) < TMath.periodic24(0x020000) );
		assertTrue( TMath.periodic24(0x002000) < TMath.periodic24(0x400000) );
		assertTrue( TMath.periodic24(0x400000) > TMath.periodic24(0x410000) );
		assertTrue( TMath.periodic24(0x410000) > TMath.periodic24(0x411000) );
	}
}
