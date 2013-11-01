package togos.lang;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Used to indicate errors encountered while parsing, compiling, or running scripts.
 */
public class ScriptError extends Exception
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Stack trace with innermost frame at the end.
	 */
	protected final List<SourceLocation> scriptTrace;
	
	public ScriptError( String message, Throwable cause, List<SourceLocation> trace ) {
		super( message, cause );
		this.scriptTrace = trace;
	}
	
	public ScriptError( String message, Throwable cause, SourceLocation sLoc ) {
		this( message, cause, sLoc == null ? Collections.<SourceLocation>emptyList() : Arrays.asList(sLoc) );
	}
	
	public ScriptError( String message, List<SourceLocation> trace ) {
		this( message, null, trace );
	}
	
	public ScriptError( String message, SourceLocation sLoc ) {
		this( message, null, sLoc );
	}
	
	public ScriptError( Throwable cause, List<SourceLocation> trace ) {
		this( null, cause, trace );
	}
	
	public ScriptError( Throwable cause, SourceLocation sLoc ) {
		this( null, cause, sLoc );
	}
	
	////
	
	protected String traceAsString(String linePrefix, String lineSeparator) {
		final StringBuilder sb = new StringBuilder();
		for( int i=scriptTrace.size()-1; i>=0; --i ) {
			if( sb.length() > 0 ) sb.append(lineSeparator);
			sb.append(linePrefix + BaseSourceLocation.toString(scriptTrace.get(i)));
		}
		return sb.toString();
	}
	
	public String getMessageWithScriptTrace() {
		return getMessage() + (
			scriptTrace.size() == 0 ?
				" (no source location)" :
				"\n" + traceAsString("  ","\n")
		);
	}
	
	public List<SourceLocation> getScriptTrace() {
		return scriptTrace;
	}
}
