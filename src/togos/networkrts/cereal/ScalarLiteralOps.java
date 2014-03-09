package togos.networkrts.cereal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.util.Float16;

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
	 *   
	 *   8-bit float is probably not useful; so 0x08 may be put to another use, maybe to mean NaN?
	 * 0x80-0xFF : small integers -64 to 63, where
	 *   0xC0 = -64, 0xFF = -1, 0x80 = 0, 0x3F = 63
	 */
	
	public static final byte SE_SHORTSTRING = 0x02;
	public static final byte SE_LONGSTRING = 0x03;
	
	public static final byte NE_FLOAT16 = 0x09;
	public static final byte NE_FLOAT32 = 0x0A;
	public static final byte NE_FLOAT64 = 0x0B;
	public static final byte NE_INT8    = 0x0C;
	public static final byte NE_INT16   = 0x0D;
	public static final byte NE_INT32   = 0x0E;
	public static final byte NE_INT64   = 0x0F;
	public static final byte NE_INT6    = (byte)0x80;
	
	public static final byte decodeSmallInt( byte dat ) {
		assert (dat & 0x80) == 0x80;
		
		return ((dat & 0x40) == 0x40) ? dat : (byte)(dat & 0x3F);
	}
	public static final byte encodeSmallInt( int v ) {
		assert fitsIn6Bits(v);
		return (byte)(0x80 | v); 
	}
	protected static boolean fitsIn6Bits( int v ) {
		return v >= -64 && v <= 63;
	}
	protected static boolean isFloatEncoding( byte encoding ) {
		switch( encoding ) {
		case NE_FLOAT16: case NE_FLOAT32: case NE_FLOAT64: return true;
		default: return false;
		}
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
			case NE_FLOAT16:
				dat16 = CerealUtil.readInt16(data, offset+1);
				ds.pushStackItem( new Float(Float16.toFloat(dat16)) );
				return offset+3;
			case NE_FLOAT32:
				dat32 = CerealUtil.readInt32(data, offset+1);
				ds.pushStackItem( new Float(Float.intBitsToFloat(dat32)) );
				return offset+5;
			case NE_FLOAT64:
				dat64 = CerealUtil.readInt64(data, offset+1);
				ds.pushStackItem( new Double(Double.longBitsToDouble(dat64)) );
				return offset+9;
			case NE_INT8:
				ds.pushStackItem( new Byte(data[offset+1]) );
				return offset+2;
			case NE_INT16:
				dat16 = CerealUtil.readInt16(data, offset+1);
				ds.pushStackItem( new Short(dat16) );
				return offset+3;
			case NE_INT32:
				dat32 = CerealUtil.readInt32(data, offset+1);
				ds.pushStackItem( new Integer(dat32) );
				return offset+5;
			case NE_INT64:
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
	
	public static final void writeInt6( byte val, OutputStream os ) throws IOException {
		os.write( encodeSmallInt(val) );
	}
	public static final void writeInt8( byte val, OutputStream os ) throws IOException {
		os.write( NE_INT8 );
		os.write( val );
	}
	public static final void writeInt16( short val, OutputStream os ) throws IOException {
		os.write( NE_INT16 );
		CerealUtil.writeInt16( val, os );
	}
	public static final void writeInt32( int val, OutputStream os ) throws IOException {
		os.write( NE_INT32 );
		CerealUtil.writeInt32( val, os );
	}
	public static final void writeInt64( long val, OutputStream os ) throws IOException {
		os.write( NE_INT64 );
		CerealUtil.writeInt64( val, os );
	}
	public static final void writeFloat16( float val, OutputStream os ) throws IOException {
		os.write( NE_FLOAT16 );
		CerealUtil.writeInt16( (short)Float16.fromFloat(val), os );
	}
	public static final void writeFloat32( float val, OutputStream os ) throws IOException {
		os.write( NE_FLOAT32 );
		CerealUtil.writeInt32( Float.floatToIntBits(val), os );
	}
	public static final void writeFloat64( double val, OutputStream os ) throws IOException {
		os.write( NE_FLOAT64 );
		CerealUtil.writeInt64( Double.doubleToLongBits(val), os );
	}
	
	
	/**
	 * Encode the given number using the specified encoding.
	 */
	public static final void writeNumber( Number num, byte encoding, OutputStream os ) throws IOException {
		switch( encoding ) {
		case NE_FLOAT16: writeFloat16( num.floatValue() , os ); return;
		case NE_FLOAT32: writeFloat32( num.floatValue() , os ); return;
		case NE_FLOAT64: writeFloat64( num.doubleValue(), os ); return;
		case NE_INT8   : writeInt8(    num.byteValue()  , os ); return;
		case NE_INT16  : writeInt16(   num.shortValue() , os ); return;
		case NE_INT32  : writeInt32(   num.intValue()   , os ); return;
		case NE_INT64  : writeInt64(   num.longValue()  , os ); return;
		case NE_INT6   : writeInt6(    num.byteValue()  , os ); return;
		default:
			throw new UnsupportedOperationException(String.format("Unsupported number encoding:, 0x%02x",encoding));
		}
	}
	/**
	 * Encode the given integer using the specified encoding.
	 */
	public static final void writeInteger( long num, byte encoding, OutputStream os ) throws IOException {
		switch( encoding ) {
		case NE_INT8   : writeInt8(   (byte)num, os ); return;
		case NE_INT16  : writeInt16( (short)num, os ); return;
		case NE_INT32  : writeInt32(   (int)num, os ); return;
		case NE_INT64  : writeInt64(        num, os ); return;
		case NE_INT6   : writeInt6(   (byte)num, os ); return;
		default:
			throw new UnsupportedOperationException(String.format("Unsupported integer encoding:, 0x%02x",encoding));
		}
	}
	/**
	 * Encode the given number using the specified encoding.
	 */
	public static final void writeFloat( double num, byte encoding, OutputStream os ) throws IOException {
		switch( encoding ) {
		case NE_FLOAT16: writeFloat16(  (float)num, os ); return;
		case NE_FLOAT32: writeFloat32(  (float)num, os ); return;
		case NE_FLOAT64: writeFloat64(         num, os ); return;
		default:
			throw new UnsupportedOperationException(String.format("Unsupported float encoding:, 0x%02x",encoding));
		}
	}
	
	protected static byte nativeEncoding( Number num ) {
		if(        num instanceof Byte ) {
			return NE_INT8;
		} else if( num instanceof Short ) {
			return NE_INT16;
		} else if( num instanceof Integer ) {
			return NE_INT32;
		} else if( num instanceof Long ) {
			return NE_INT64;
		} else if( num instanceof Float ) {
			return NE_FLOAT32;
		} else {
			// Use a double to approximate anything else
			return NE_FLOAT64;
		}
	}
	
	/**
	 * Write the number in its 'native' form, based on type.
	 * e.g. a Double will be written using 9 bytes, even if the number it represents
	 * could be written more compactly.  This is mainly for testing.  Normally you should use
	 * writeCompactNumber. 
	 */
	public static final void writeNumberNative( Number num, OutputStream os ) throws IOException {
		writeNumber( num, nativeEncoding(num), os );
	}
	
	protected static boolean canBeRepresentedAsLong( Number num ) {
		 return num.longValue() == num.doubleValue();
	}
	protected static boolean canBeRepresentedAsLong( double num ) {
		 return (long)num == num;
	}
	
	protected static byte optimalEncoding( long val ) {
		if( (short)val == val ) {
			if( fitsIn6Bits( (short)val ) ) return NE_INT6;
			if( (byte)val == val ) return NE_INT8;
			return NE_INT16;
		}
		if( (int)val == val ) return NE_INT32;
		return NE_INT64;
	}
	protected static byte optimalEncoding( double val ) {
		if( canBeRepresentedAsLong(val) ) {
			return optimalEncoding( (long)val );
		}
		
		float fVal = (float)val;
		if( fVal == val ) {
			float squashed16 = Float16.toFloat( Float16.fromFloat( fVal ) );
			return squashed16 == fVal ? NE_FLOAT16 : NE_FLOAT32;
		}
		return NE_FLOAT64;
	}
	protected static byte optimalEncoding( Number val ) {
		return canBeRepresentedAsLong(val) ?
			optimalEncoding(val.longValue()  ):
			optimalEncoding(val.doubleValue());
	}
	
	/**
	 * Encode the given number in the most space-efficient representation.
	 */
	public static final void writeNumberCompact( double num, OutputStream os ) throws IOException {
		byte optimalEncoding = optimalEncoding(num);
		if( isFloatEncoding(optimalEncoding) ) {
			writeFloat( num, optimalEncoding, os );
		} else {
			writeInteger( (long)num, optimalEncoding, os );
		}
	}
	
	/**
	 * Encode the given number in the most space-efficient representation.
	 */
	public static final void writeNumberCompact( long num, OutputStream os ) throws IOException {
		writeInteger( num, optimalEncoding(num), os );
	}
	
	/**
	 * Encode the given number in the most space-efficient representation.
	 */
	public static final void writeNumberCompact( Number num, OutputStream os ) throws IOException {
		byte optimalEncoding = optimalEncoding(num);
		if( isFloatEncoding(optimalEncoding) ) {
			writeFloat( num.doubleValue(), optimalEncoding, os );
		} else {
			writeInteger( num.longValue(), optimalEncoding, os );
		}
	}
	
	public static void writeByteString( byte[] data, OutputStream os ) throws IOException {
		if( data.length <= 255 ) {
			os.write( SE_SHORTSTRING );
			os.write( (byte)data.length );
			os.write( data );
		} else if( data.length <= 65535 ) {
			os.write( SE_LONGSTRING );
			CerealUtil.writeInt16( (short)data.length, os );
			os.write( data );
		} else {
			throw new UnsupportedOperationException("Byte string to be encoded is too long (>65535, "+data.length+" to be exact)");
		}
	}
	
	public static void writeValue( Object thing, OutputStream os ) throws IOException {
		if( thing instanceof byte[] ) {
			writeByteString( (byte[])thing, os );
		} else if( thing instanceof Number ) {
			writeNumberCompact( (Number)thing, os );
		} else {
			throw new UnsupportedOperationException("Don't know how to encode "+thing.getClass());
		}
	}
}
