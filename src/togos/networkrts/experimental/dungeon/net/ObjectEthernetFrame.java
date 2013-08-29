package togos.networkrts.experimental.dungeon.net;

public class ObjectEthernetFrame<T>
{
	public final long srcAddress, destAddress;
	public final T payload;
	
	public ObjectEthernetFrame( long srcAddress, long destAddress, T payload ) {
		this.srcAddress = srcAddress;
		this.destAddress = destAddress;
		this.payload = payload;
	}
}
