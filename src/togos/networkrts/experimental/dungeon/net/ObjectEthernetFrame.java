package togos.networkrts.experimental.dungeon.net;

public class ObjectEthernetFrame<T>
{
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final Class<ObjectEthernetFrame<?>> GENERIC_CLASS = (Class<ObjectEthernetFrame<?>>)(Class)ObjectEthernetFrame.class;
	
	public final long srcAddress, destAddress;
	public final T payload;
	
	public ObjectEthernetFrame( long srcAddress, long destAddress, T payload ) {
		this.srcAddress = srcAddress;
		this.destAddress = destAddress;
		this.payload = payload;
	}
}
