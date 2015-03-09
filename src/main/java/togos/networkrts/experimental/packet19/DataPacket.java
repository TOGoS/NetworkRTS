package togos.networkrts.experimental.packet19;

import togos.blob.ByteChunk;

public interface DataPacket extends ByteChunk
{
	public String toAtomicString();
}
