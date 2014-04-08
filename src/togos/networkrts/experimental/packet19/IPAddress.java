package togos.networkrts.experimental.packet19;

import togos.blob.SimpleByteChunk;

public abstract class IPAddress extends SimpleByteChunk
{
	public IPAddress( byte[] data, int offset, int length ) {
		super(data, offset, length);
	}
	public IPAddress( byte[] data ) {
		super(data);
	}
}
