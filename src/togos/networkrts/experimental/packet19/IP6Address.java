package togos.networkrts.experimental.packet19;

import togos.blob.SimpleByteChunk;

public class IP6Address extends SimpleByteChunk
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
