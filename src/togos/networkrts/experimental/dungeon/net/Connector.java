package togos.networkrts.experimental.dungeon.net;

public interface Connector<Payload>
{
	public ConnectorType getConnectorType();
	public Class<Payload> getPayloadClass();
	public String getDescription();
	/**
	 * Don't call this directly.
	 * Use Connectors.connect(...)
	 * */
	public void connectedTo(Connector<Payload> other);
	public Connector<Payload> getMate();
	/**
	 * Called when a message is incoming from the mate side.
	 * To send a message outward, getMate().messageReceived(message)
	 */
	public void messageReceived( Payload message );
	/**
	 * A connector cannot be connected or disconnected if it is locked.
	 */
	public boolean isLocked();
}
