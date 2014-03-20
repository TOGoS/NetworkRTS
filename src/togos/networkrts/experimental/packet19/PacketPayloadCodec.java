package togos.networkrts.experimental.packet19;

import java.io.IOException;
import java.io.OutputStream;

public interface PacketPayloadCodec<T>
{
	public void encode( T obj, OutputStream os ) throws IOException;
	public T decode( byte[] data, int offset, int length ) throws MalformedDataException;
}
