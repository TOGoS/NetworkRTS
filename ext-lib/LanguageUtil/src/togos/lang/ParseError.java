package togos.lang;

import java.util.List;

public class ParseError extends ScriptError
{
	private static final long serialVersionUID = 1L;
	
	public ParseError( String message, Throwable cause, List<SourceLocation> trace ) {
		super( message, cause, trace );
	}
	
	public ParseError( String message, SourceLocation sloc ) {
		super( message, sloc );
	}
	
	public ParseError( SourceLocation sLoc, Exception cause ) {
		super( cause, sLoc );
	}
}
