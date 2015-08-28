package togos.networkrts.experimental.forthbots.lang;

import togos.lang.BaseSourceLocation;
import togos.lang.ParseError;
import togos.lang.ScriptError;
import togos.lang.SourceLocation;

public class Tokenizer
{
	enum State {
		NO_TOKEN,
		WORD_BOUNDARY, // Just read a bareword or quoted string
		BAREWORD( NO_TOKEN, Token.Type.BAREWORD ),
		SINGLE_QUOTED_STRING( NO_TOKEN, Token.Type.SINGLE_QUOTED_STRING ),
		SINGLE_QUOTED_STRING_ESCAPE( SINGLE_QUOTED_STRING ),
		DOUBLE_QUOTED_STRING( NO_TOKEN, Token.Type.DOUBLE_QUOTED_STRING ),
		DOUBLE_QUOTED_STRING_ESCAPE( DOUBLE_QUOTED_STRING ),
		LINE_COMMENT;
		
		public final State parentState;
		public final Token.Type tokenType;
		State( State parentState, Token.Type tokenType ) {
			this.parentState = parentState;
			this.tokenType = tokenType;
		}
		State( State parentState ) {
			this( parentState, null );
		}
		State() {
			this( null );
		}
	};
	
	protected final Handler<Token,ScriptError> tokenHandler;
	public String filename = "unknown source";
	public int lineNumber = 1, columnNumber = 1;
	/** Location of first character of current token */
	protected int ctLineNumber, ctColumnNumber;
	protected char[] tokenBuffer = new char[1024];
	protected int length = 0;
	protected State state = State.NO_TOKEN;
	protected final int tabWidth;
	
	public Tokenizer( String filename, int line, int col, int tabWidth, Handler<Token,ScriptError> tokenHandler ) {
		this.filename = filename;
		this.lineNumber = line;
		this.columnNumber = col;
		this.tabWidth = tabWidth;
		this.tokenHandler = tokenHandler;
	}
	
	public Tokenizer( SourceLocation sLoc, int tabWidth, Handler<Token,ScriptError> tokenHandler ) {
		setSourceLocation(sLoc);
		this.tabWidth = tabWidth;
		this.tokenHandler = tokenHandler;
	}
	
	public void setSourceLocation( String filename ) {
		setSourceLocation( filename, 1, 1 );
	}
	
	public void setSourceLocation( String filename, int l, int c ) {
		this.filename = filename;		
		this.lineNumber = l;
		this.columnNumber = c;
	}
	
	public void setSourceLocation( SourceLocation sLoc ) {
		setSourceLocation( sLoc.getSourceFilename(), sLoc.getSourceLineNumber(), sLoc.getSourceColumnNumber() );
	}
	
	public SourceLocation getSourceLocation() {
		return new BaseSourceLocation( filename, lineNumber, columnNumber );
	}
	
	protected static boolean isWhitespace( char c ) {
		switch( c ) {
		case ' ': case '\t': case '\r': case '\n':
			return true;
		default:
			return false;
		}
	}
	
	protected static boolean isQuote( char c ) {
		return c == '\'' || c == '"';
	}
	
	protected static boolean isComment( char c ) {
		return c == '#';
	}
	
	protected static boolean isWordChar( char c ) {
		return !isWhitespace(c) && !isQuote(c) && !isComment(c);
	}
	
	protected void handle( char c ) throws Exception {
		switch( state ) {
		case SINGLE_QUOTED_STRING_ESCAPE:
		case DOUBLE_QUOTED_STRING_ESCAPE:
			switch( c ) {
			case '\\': case '\'': case '"':
				tokenBuffer[length++] = c;
				break;
			case 'n':
				tokenBuffer[length++] = '\n';
				break;
			case 'r':
				tokenBuffer[length++] = '\r';
				break;
			case 't':
				tokenBuffer[length++] = '\t';
				break;
			default:
				throw new ParseError("Invalid escape character: '"+c+"'", getSourceLocation());
			}
			state = state.parentState;
			break;
		case SINGLE_QUOTED_STRING:
			switch( c ) {
			case '\'':
				newToken( State.WORD_BOUNDARY );
				break;
			case '\\':
				state = State.SINGLE_QUOTED_STRING_ESCAPE;
				break;
			default:
				tokenBuffer[length++] = c;
			}
			break;
		case DOUBLE_QUOTED_STRING:
			switch( c ) {
			case '"':
				newToken( State.WORD_BOUNDARY );
				break;
			case '\\':
				state = State.DOUBLE_QUOTED_STRING_ESCAPE;
				break;
			default:
				tokenBuffer[length++] = c;
			}
			break;
		case LINE_COMMENT:
			if( c == '\n' ) state = State.NO_TOKEN;
			break;
		case BAREWORD:
			if( isQuote(c) ) {
				throw new ParseError("No quotes allowed here; add some whitespace!", getSourceLocation());
			} else if( isWhitespace(c) ) {
				newToken( State.NO_TOKEN );
			} else if( isComment(c) ) {
				newToken( State.LINE_COMMENT );
			} else {
				tokenBuffer[length++] = c;
			}
			break;
		case WORD_BOUNDARY:
			if( isQuote(c) || isWordChar(c) ) {
				throw new ParseError("No quotes allowed here; add some whitespace!", getSourceLocation());
			}
			// Intentional fall-through!
		case NO_TOKEN:
			if( c == '#' ) {
				state = State.LINE_COMMENT;
			} else if( c == '"' ) {
				newToken( State.DOUBLE_QUOTED_STRING );
			} else if( c == '\'' ) {
				newToken( State.SINGLE_QUOTED_STRING );
			} else if( isComment(c) ) {
				newToken( State.LINE_COMMENT );
			} else if( isWhitespace(c) ) {
				state = State.NO_TOKEN;
			} else {
				newToken( State.BAREWORD, c );
			}
			break;
		default:
			throw new ParseError("Invalid tokenizer state: "+state, getSourceLocation());
		}
		
		if( c == '\t' ) {
			columnNumber += tabWidth;
		} else if( c == '\n' ) {
			lineNumber += 1;
			columnNumber = 1;
		} else {
			++columnNumber;
		}
	}
	
	protected void newToken( State newState, char firstChar ) throws Exception {
		newToken( newState );
		tokenBuffer[length++] = firstChar;
	}
	
	protected void newToken( State newState ) throws Exception {
		flushToken();
		state = newState;
		length = 0;
		ctLineNumber = lineNumber;
		ctColumnNumber = columnNumber;
	}
	
	protected void flushToken() throws ScriptError {
		if( state.tokenType != null ) {
			String tokenText = new String(tokenBuffer,0,length);
			tokenHandler.handle( new Token( state.tokenType, tokenText, filename, ctLineNumber, ctColumnNumber ) );
		}
	}
	
    public void handle( char[] value, int len ) throws Exception {
    	for( int i=0; i<len; ++i ) handle(value[i]);
    }
    
    public void handle( char[] value ) throws Exception {
    	handle( value, value.length );
    }
    
    public void handle( String data ) throws Exception {
    	handle( data.toCharArray() );
    }
    
    public void end() throws Exception {
    	flushToken();
    }
}
