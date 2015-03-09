package togos.networkrts.util;

public class ResourceNotFound extends Exception
{
	private static final long serialVersionUID = 1L;

	public ResourceNotFound( String uri, Throwable cause ) {
		super("Resource not found: "+uri, cause);
	}
	
	public ResourceNotFound( String uri ) {
		super("Resource not found: "+uri);
	}
}
