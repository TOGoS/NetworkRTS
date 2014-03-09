package togos.networkrts.cereal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import togos.networkrts.cereal.CerealDecoder.DecodeState;

public class ScalarLiteralOps extends OperationLibrary
{
	/*
	 * 0x02 : small byte string, length given by next byte
	 * 0x03 : long byte string, length given by next 2 bytes
	 * 0x04-0x07 : maybe reserved for unsigned integers
	 * 0x08-0x0F : integer and floating-point numbers
	 *   of bits 1abc,
	 *   a indicates type: signed int (1) or float (0)
	 *   b c indicate size: 8-bit (0), 16-bit (1), 32-bit (2), or 64-bit (4)
	 * 0x80-0xFF : small integers -64 to 63, where
	 *   0xC0 = -64, 0xFF = -1, 0x80 = 0, 0x3F = 63
	 */
	
	public static final byte decodeSmallInt( byte dat ) {
		assert (dat & 0x80) == 0x80;
		
		return ((dat & 0x40) == 0x40) ? dat : (byte)(dat & 0x3F);
	}
	public static final byte encodeSmallInt( int v ) {
		assert v <= 63;
		assert v >= -64;
		
		return (byte)(0x80 | v); 
	}
	
	public static final OperationType STRING = new OperationType() {
		@Override public int apply( byte[] data, int offset, DecodeState ds, CerealDecoder context ) throws InvalidEncoding {
			int length; byte[] string;
			switch( data[offset] ) {
			case 0x02:
				length = data[offset+1] & 0xFF;
				string = CerealUtil.extract(data, offset+2, length);
				ds.pushStackItem(string);
				return offset+2+length;
			case 0x03:
				length = CerealUtil.readInt16(data, offset+1) & 0xFFFF;
				string = CerealUtil.extract(data, offset+3, length);
				ds.pushStackItem(string);
				return offset+3+length;
			default: return offset;
			}
		}
	};
	public static final OperationType NUMBER = new OperationType() {
		@Override public int apply( byte[] data, int offset, DecodeState ds, CerealDecoder context ) {
			final byte dat = data[offset];
			short dat16; int dat32; long dat64;
			switch( dat ) {
			case 0x08: throw new UnsupportedOperationException("8-bit float not supported");
			case 0x09: throw new UnsupportedOperationException("16-bit float not supported");
			case 0x0A:
				dat32 = CerealUtil.readInt32(data, offset+1);
				ds.pushStackItem( new Float(Float.intBitsToFloat(dat32)) );
				return offset+5;
			case 0x0B:
				dat64 = CerealUtil.readInt64(data, offset+1);
				ds.pushStackItem( new Double(Double.longBitsToDouble(dat64)) );
				return offset+9;
			case 0x0C:
				ds.pushStackItem( new Byte(data[offset+1]) );
				return offset+2;
			case 0x0D:
				dat16 = CerealUtil.readInt16(data, offset+1);
				ds.pushStackItem( new Short(dat16) );
				return offset+3;
			case 0x0E:
				dat32 = CerealUtil.readInt32(data, offset+1);
				ds.pushStackItem( new Integer(dat32) );
				return offset+5;
			case 0x0F:
				dat64 = CerealUtil.readInt64(data, offset+1);
				ds.pushStackItem( new Long(dat64) );
				return offset+9;
			default: return offset;
			}
		}
	};
	public static final OperationType SMALLINT = new OperationType() {
		@Override public int apply( byte[] data, int offset, DecodeState ds, CerealDecoder context ) {
			final byte dat = data[offset];
			if( (dat & 0x80) == 0x80 ) {
				ds.pushStackItem( new Byte(decodeSmallInt(dat)) );
				return offset+1;
			}
			return offset;
		}
	};
	
	protected static final ArrayList<OperationType> SCALAR_LITERAL_OPS = new ArrayList<OperationType>();
	static {
		SCALAR_LITERAL_OPS.add(STRING);
		SCALAR_LITERAL_OPS.add(NUMBER);
		SCALAR_LITERAL_OPS.add(SMALLINT);
	}
	private ScalarLiteralOps() {
		super( "urn:bitprint:22GN6OMDUBQMVAAMOBJYN7VESN3N2XQ5.X2N47ZFVJCN4HJMFRKZQSMBSGCN6Z7PYKXITWDA", SCALAR_LITERAL_OPS );
	}
	
	public static final ScalarLiteralOps INSTANCE = new ScalarLiteralOps();
	
	/**
	 * Encode the given number in the most space-efficient representation.
	 */
	public static final void writeNumber( double num, OutputStream os ) throws IOException {
		os.write(0x0F);
		CerealUtil.writeInt64(Double.doubleToLongBits(num), os);
	}
}
