package togos.networkrts.experimental.packet19;

public class PacketWrapping<T>
{
	public final PacketWrapping<?> parent;
	public final T payload;
	
	public PacketWrapping( PacketWrapping<?> parent, T payload ) {
		this.parent = parent;
		this.payload = payload;
	}
	
	public PacketWrapping( T payload ) {
		this( null, payload );
	}
	
	public <X> X getPayload(Class<X> c) {
		return c.cast(payload);
	}
}
