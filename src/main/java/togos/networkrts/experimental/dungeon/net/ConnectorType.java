package togos.networkrts.experimental.dungeon.net;

public interface ConnectorType
{
	public String getName();
	public boolean canConnectTo(ConnectorType other);
}
