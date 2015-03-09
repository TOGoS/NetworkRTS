package togos.networkrts.experimental.inet.transport;

public interface TunnelCodec<InnerPacket, OuterPacket>
{
	public OuterPacket encode( InnerPacket p );
	public InnerPacket decode( OuterPacket p );
}
