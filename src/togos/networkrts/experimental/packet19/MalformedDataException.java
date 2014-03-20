package togos.networkrts.experimental.packet19;

public class MalformedDataException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public MalformedDataException( String message ) {
		super(message);
	}
	public MalformedDataException( String message, Throwable cause ) {
		super(message, cause);
	}
}
