package togos.networkrts.util;

/**
 * A bit address is a 64-bit integer.
 * It does not need to be unique.
 * It may contain both flags (usually upper 32 bits) and identification (lower 32).
 * 
 * Address ranges are 64-dimensional; each bit is compared separately.
 * 
 * The specific address NO_ADDRESS=0
 * indicates 'no address', so is treated specially by functions
 * that work with specific addresses (as opposed to ranges, in
 * which that value has no special meaning).
 * 
 * Ranges where the min and max are of size 1.
 * To make an address range that contains nothing, max must be < min.
 * This is usually accomplished using MAX_ADDRESS..MIN_ADDRESS,
 * the inverse of MIN_ADDRESS..MAX_ADDRESS, which includes everything.
 * 
 * This class contains utilities for handling generic addresses
 * without making any assumptions about their internal structure
 * other than the special case for NO_ADDRESS.
 */
public class BitAddressUtil
{
	public static final long MIN_ADDRESS = 0;
	public static final long MAX_ADDRESS = -1;
	/**
	 * Special 'null' ID for objects without an ID
	 * Must be explicitly handled
	 * */
	public static final long NO_ADDRESS = 0;
	
	public static final boolean bitwiseGte( long a, long b ) {
		return (a | b) == a;
	}
	
	public static final boolean bitwiseLte( long a, long b ) {
		return (a & b) == a;
	}
	
	public static boolean rangesIntersect( long min0, long max0, long min1, long max1 ) {
		return bitwiseGte( max0, min1 ) && bitwiseGte( max1, min0 );
	}
	
	public static boolean rangesIntersect( BitAddressRange range0, long min1, long max1 ) {
		return rangesIntersect(
			range0.getMinBitAddress(), range0.getMaxBitAddress(),
			min1, max1
		);
	}
	
	public static boolean rangesIntersect( BitAddressRange a, BitAddressRange b ) {
		return rangesIntersect(
			a.getMinBitAddress(), a.getMaxBitAddress(),
			b.getMinBitAddress(), b.getMaxBitAddress()
		);
	}
	
	public static boolean rangeContains( long min, long max, long v ) {
		return v != NO_ADDRESS && bitwiseGte( max, v ) && bitwiseGte( v, min ); 
	}
	
	public static boolean rangeContains( BitAddressRange r, long v ) {
		return rangeContains( r.getMinBitAddress(), r.getMaxBitAddress(), v );
	}
	
	public static long toMinAddress( long addy ) {
		return addy == NO_ADDRESS ? MAX_ADDRESS : addy;
	}
	public static long toMaxAddress( long id ) {
		return id == NO_ADDRESS ? MIN_ADDRESS : id;
	}
	
	public static long minAddress( long a, long b ) {
		return toMinAddress(a) & toMinAddress(b);
	}
	public static long maxAddress( long a, long b ) {
		return toMaxAddress(a) | toMaxAddress(b);
	}
}
