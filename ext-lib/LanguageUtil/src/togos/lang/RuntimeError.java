package togos.lang;

import java.util.List;

public class RuntimeError extends ScriptError
{
	private static final long serialVersionUID = 1L;
	
	public RuntimeError( String message, Throwable cause, List<SourceLocation> trace ) {
		super( message, cause, trace );
	}
	
	public RuntimeError( String message, List<SourceLocation> trace ) {
		super( message, trace );
	}
	
	public RuntimeError( Throwable cause, List<SourceLocation> trace ) {
		super( cause, trace );
	}
}
