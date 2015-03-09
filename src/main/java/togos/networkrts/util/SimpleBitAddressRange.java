package togos.networkrts.util;

public class SimpleBitAddressRange implements BitAddressRange
{
	public final long min, max;
	
	public SimpleBitAddressRange( long min, long max ) {
		this.min = min;
		this.max = max;
	}
	
	@Override public long getMinBitAddress() { return min; }
	@Override public long getMaxBitAddress() { return max; }
}
