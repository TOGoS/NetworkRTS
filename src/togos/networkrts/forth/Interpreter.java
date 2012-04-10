package togos.networkrts.forth;

import togos.blob.SimpleByteChunk;

public class Interpreter
{
	/** Duplicate the value from position N */
	final static byte OP_DUPFROML     = 0x10; // () -- (value) 
	
	// Ops to push literals - these all push a single value onto the stack.
	final static byte OP_PUSH_INT8    = 0x41;
	final static byte OP_PUSH_INT16   = 0x43;
	final static byte OP_PUSH_INT32   = 0x43;
	final static byte OP_PUSH_INT64   = 0x44;
	final static byte OP_PUSH_FLOAT32 = 0x45;
	final static byte OP_PUSH_FLOAT64 = 0x46;
	final static byte OP_PUSH_STRING  = 0x48;
	
	final static byte OP_CLEAR_CACHE  = 0x50;
	final static byte OP_STORE_CACHE  = 0x51; // (value name) -- ()
	final static byte OP_FETCH_CACHE  = 0x52; // (name) -- (value)
	
	final static byte OP_CLEAR_STATE  = 0x54;
	final static byte OP_STORE_STATE  = 0x55; // (value name) -- ()
	
	static int decodeInt32( byte[] buf, int offset ) {
		return
			((int)(buf[offset+0]) << 24) |
			((int)(buf[offset+1]) << 16) |
			((int)(buf[offset+2]) <<  8) |
			((int)(buf[offset+3]) <<  0);
	}
	
	void step( State s ) {
		byte opcode = s.program[s.programPosition];
		
		switch( opcode ) {
		case( OP_DUPFROML ):
			int n = s.program[s.programPosition+1];
			s.stack.add( s.stack.get(n < 0 ? s.stack.size()+n : n));
			s.programPosition += 2;
		case( OP_PUSH_INT8 ):
			s.stack.add( new Byte(s.program[s.programPosition+1]) );
			s.programPosition += 2;
			break;
		case( OP_PUSH_INT32 ):
			s.stack.add( new Integer(decodeInt32(s.program,s.programPosition+1)));
			s.programPosition += 5;
			break;
		case( OP_PUSH_STRING ):
			int strlen = decodeInt32(s.program,s.programPosition+1);
			new SimpleByteChunk( s.program, s.programPosition+5, strlen );
			s.programPosition += s.programPosition + 5 + strlen;
			break;
		default:
			throw new RuntimeException("Unsupported opcode "+opcode);
		}
	}
}
