package togos.lang;

import java.util.List;

public class CompileError extends ScriptError
{
	private static final long serialVersionUID = 1L;
	
	public CompileError( String message, Throwable cause, List<SourceLocation> trace ) {
		super( message, cause, trace );
	}
	
	public CompileError( String message, SourceLocation sloc ) {
		super( message, sloc );
	}
	
	public CompileError( SourceLocation sLoc, Exception cause ) {
		super( cause, sLoc );
	}
}
