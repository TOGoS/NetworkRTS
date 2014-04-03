package togos.networkrts.experimental.packet19;

import java.io.IOException;

public abstract class DataPacketPayloadCodec<T extends DataPacket> implements PacketPayloadCodec<T>
{
	public void encode(T obj, java.io.OutputStream os) throws IOException {
		os.write(obj.getBuffer(), obj.getOffset(), obj.getSize());
	};
}
