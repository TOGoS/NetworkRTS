package togos.networkrts.experimental.packet19;

import togos.blob.util.SimpleByteChunk;

public abstract class IPAddress extends SimpleByteChunk
{
	public IPAddress( byte[] data, int offset, int length ) {
		super(data, offset, length);
	}
	public IPAddress( byte[] data ) {
		super(data);
	}
	
	public abstract boolean matches( IPAddress other );
}
