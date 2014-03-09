package togos.networkrts.cereal;

// TODO: This is pretty generic and could be moved to indicate as such
public class InvalidEncoding extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public InvalidEncoding( String message, Throwable cause ) {
		super(message, cause);
	}
	public InvalidEncoding( String message ) {
		super(message);
	}
}
