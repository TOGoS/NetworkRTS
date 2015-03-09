package togos.networkrts.experimental.dungeon;

public interface MessageReceiver<T>
{
	public void messageReceived( T message );
}
