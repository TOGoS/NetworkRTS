package togos.networkrts.experimental.dungeon.net;

public class Connectors
{
	protected static void _disconnect( Connector<?> c ) {
		if( c != null ) {
			c.connectedTo(null);
		}
	}
	
	protected static boolean canBeReconnected(Connector<?> c) {
		return c == null || (!c.isLocked() && (c.getMate() == null || !c.getMate().isLocked()));  
	}
	
	protected static void ensureReconnectable( Connector<?> c ) throws ConnectionError {
		if( !canBeReconnected(c) ) {
			throw new ConnectionError(c.getDescription()+" is stuck");
		}
	}
	
	public static <T> void forceDisconnect(Connector<T> c) {
		_disconnect(c.getMate());
		_disconnect(c);
	}
	
	public static void disconnect( Connector<?> c ) throws ConnectionError {
		ensureReconnectable(c);
		forceDisconnect( c );
	}
	
	public static boolean canConnect(Connector<?> c1, Connector<?> c2) {
		if( c1 == null || c2 == null ) return true;
		
		return c1.getPayloadClass() == c2.getPayloadClass() &&
			c1.getConnectorType().canConnectTo(c2.getConnectorType());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" }) // It's checked dynamically based on payloadClass.
	public static void connect( Connector c1, Connector c2 ) throws ConnectionError {
		if( c1.getMate() == c2 ) return;
		
		disconnect(c1);
		disconnect(c2);
		if( !canConnect(c1,c2) ) {
			throw new ConnectionError("Can't connect "+c1.getDescription()+" to "+c2.getDescription());
		}
		c1.connectedTo(c2);
		c2.connectedTo(c1);
	}
}
