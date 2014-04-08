package togos.networkrts.experimental.packet19;

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
}
