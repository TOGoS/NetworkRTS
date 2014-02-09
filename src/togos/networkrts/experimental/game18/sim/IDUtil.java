package togos.networkrts.experimental.game18.sim;


public class IDUtil
{
	public static final long MIN_ID = 0;
	public static final long MAX_ID = -1;
	/**
	 * Special 'null' ID for objects without an ID
	 * Must be explicitly handled
	 * */
	public static final long NO_ID = 0;
	
	public static final boolean bitwiseGte( long a, long b ) {
		return (a | b) == a; 
	}
	
	public static final boolean bitwiseLte( long a, long b ) {
		return (a & b) == a; 
	}
	
	public static boolean rangesIntersect( long min0, long max0, long min1, long max1 ) {
		return bitwiseGte( max0, min1 ) && bitwiseGte( max1, min0 ); 
	}
	
	public static boolean rangeContains( long min, long max, long v ) {
		return v != NO_ID && bitwiseGte( max, v ) && bitwiseGte( v, min ); 
	}
	
	public static long toMinId( long id ) {
		return id == NO_ID ? MAX_ID : id;
	}
	public static long toMaxId( long id ) {
		return id == NO_ID ? MIN_ID : id;
	}
	
	public static long minId( long a, long b ) {
		return toMinId(a) & toMinId(b);
	}
	public static long maxId( long a, long b ) {
		return toMaxId(a) | toMaxId(b);
	}
}
