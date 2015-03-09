package togos.networkrts.util;

public final class TMath
{
	private static final short[] PERIODIC_TABL = new short[256];
	static {
		for( int i=0; i<256; ++i ) {
			PERIODIC_TABL[i] = (short)(32767 * Math.sin( (double)i * Math.PI * 2 / 256 ));
		}
	}
	
	protected static final float TWO_TO_39 = (float)Math.pow(2, 39);
	
	/**
	 * Approximates Math.sin( input * Math.PI * 2 / 0x1000000 ) * 32767 * 65536
	 * for applications where precision doesn't matter and is ~2 orders of
	 * magnitude faster.
	 *
	 * 24_32 refers to 24 bytes of input (unsigned), 32 of output (signed)
	 */
	public static final int periodic24_32( int input ) {
		final int idx1 = (input >> 16)&0xFF;
		final int idx2 = (idx1 + 1)&0xFF;
		final int w2 = input & 0xFFFF;
		final int w1 = 0x10000-w2;
		return PERIODIC_TABL[idx1] * w1 + PERIODIC_TABL[idx2] * w2;
	}
	
	/**
	 * Approximates Math.sin( input * Math.PI * 2 / 0x1000000 ) for applications
	 * where precision doesn't matter and is ~2 orders of magnitude faster.
	 */
	public static final float periodic24( int input ) {
		return (float)periodic24_32( input ) / (65536*32767);
	}
	
	public static final float periodic( long input, long period ) {
		return periodic24( (int)((input << 24) / period) );
	}
}
