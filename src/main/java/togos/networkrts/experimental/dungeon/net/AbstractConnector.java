package togos.networkrts.experimental.dungeon.net;

import togos.networkrts.experimental.dungeon.MessageReceiver;

public abstract class AbstractConnector<Payload> implements Connector<Payload>
{
	protected final ConnectorType connectorType;
	protected final Class<Payload> payloadClass;
	protected Connector<Payload> mate;
	
	public final MessageReceiver<Payload> backside = new MessageReceiver<Payload>() {
		@Override public void messageReceived(Payload message) {
			sendMessage(message);
		}
	};
	
	public AbstractConnector(ConnectorType cType, Class<Payload> payloadClass) {
		this.connectorType = cType;
		this.payloadClass = payloadClass;
	}
	
	@Override public ConnectorType getConnectorType() {
		return connectorType;
	}
	
	@Override public Class<Payload> getPayloadClass() {
		return payloadClass;
	}
	
	@Override public Connector<Payload> getMate() {
		return mate;
	}
	
	@Override public String getDescription() {
		return getPayloadClass().getName()+" "+getConnectorType().getName();
	}
	
	@Override public void connectedTo(Connector<Payload> other) {
		if( this.mate == other ) return;
		this.mate = other;
	}
	
	public void sendMessage(Payload p) {
		if( mate != null ) mate.messageReceived(p);
	}
}
