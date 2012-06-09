package togos.networkrts.inet;

public class AddressUtil
{
	public static String formatMacAddress( byte[] addy, int offset, int count ) {
		String rez = "";
		for( int i=0; i<count; ++i ) {
			if( i > 0 ) rez += ":";
			rez += Integer.toHexString(addy[offset+i]&0xFF);
		}
		return rez;
	}
	
	public static String formatMacAddress( byte[] addy ) {
		return formatMacAddress( addy, 0, addy.length );
	}
	
	public static String formatIp6Address( byte[] addy, int offset ) {
		int[] parts = new int[8];
		for( int i=0; i<8; ++i ) {
			parts[i] = ((addy[offset+i*2]&0xFF) << 8) | (addy[offset+i*2+1]&0xFF);
		}
		String rez = "";
		for( int i=0; i<8; ++i ) {
			if( i > 0 ) rez += ":";
			rez += Integer.toHexString(parts[i]);
		}
		return rez;
	}
}
