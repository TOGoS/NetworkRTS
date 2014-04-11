package togos.networkrts.experimental.packet19;

import togos.networkrts.util.ByteUtil;

public class IP6Address extends IPAddress
{
	private static final byte[] assertLongEnough( byte[] data, int offset ) {
		assert data.length >= offset+16;
		return data;
	}
	
	public IP6Address(byte[] buf, int offset) {
		super(assertLongEnough(buf, offset), offset, 16);
	}
	public IP6Address(byte[] buf) {
		this(buf, 0);
	}
	
	public long getUpper64() {
		return ByteUtil.decodeInt64(buffer, offset);
	}
	
	public long getLower64() {
		return ByteUtil.decodeInt64(buffer, offset+8);
	}
	
	public String toString() {
		String f = String.format("%x:%x:%x:%x:%x:%x:%x:%x",
			ByteUtil.decodeUInt16(buffer, offset+ 0),
			ByteUtil.decodeUInt16(buffer, offset+ 2),
			ByteUtil.decodeUInt16(buffer, offset+ 4),
			ByteUtil.decodeUInt16(buffer, offset+ 6),
			ByteUtil.decodeUInt16(buffer, offset+ 8),
			ByteUtil.decodeUInt16(buffer, offset+10),
			ByteUtil.decodeUInt16(buffer, offset+12),
			ByteUtil.decodeUInt16(buffer, offset+14)
		).replaceFirst(":0:", "::");
		while( f.contains("::0:") ) {
			f = f.replace("::0:", "::");
		}
		return f;
	}
	
	public boolean matches( IPAddress dest ) {
		if( equals(dest) ) return true;
		
		if( dest instanceof IP6Address ) {
			IP6Address dest6 = (IP6Address)dest;
			long hi = getUpper64(), lo = getLower64();
			// solicited node multicast?
			if( hi == 0xFF02000000000000l &&
				(lo & 0xFFFFFFFFFF000000l) == 0x00000001FF000000l &&
				(lo & 0x0000000000FFFFFFl) == (dest6.getLower64() & 0x0000000000FFFFFFl) ) return true;
			
		}
		return false;
	}
}
