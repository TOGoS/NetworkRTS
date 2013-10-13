package togos.networkrts.experimental.dungeon.net;

public class ConnectionError extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public ConnectionError( String message ) {
		super(message);
	}
}
