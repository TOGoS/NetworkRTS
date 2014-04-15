package togos.networkrts.cereal;

import java.io.IOException;
import java.io.OutputStream;

import togos.networkrts.cereal.op.PushDefault;
import togos.networkrts.cereal.op.PushFalse;
import togos.networkrts.cereal.op.PushFloat16;
import togos.networkrts.cereal.op.PushFloat32;
import togos.networkrts.cereal.op.PushFloat64;
import togos.networkrts.cereal.op.PushInt16;
import togos.networkrts.cereal.op.PushInt32;
import togos.networkrts.cereal.op.PushInt64;
import togos.networkrts.cereal.op.PushInt8;
import togos.networkrts.cereal.op.PushMediumString;
import togos.networkrts.cereal.op.PushSHA1BlobReference;
import togos.networkrts.cereal.op.PushSHA1ObjectReference;
import togos.networkrts.cereal.op.PushShortString;
import togos.networkrts.cereal.op.PushTrue;
import togos.networkrts.util.Float16;
import togos.networkrts.util.HasURI;

public class StandardValueOps
{
	/*
	 * Default opcode numbers for pushing literal values
	 * 
	 * 0x41 : the load opcode opcode
	 * 0x42 : small byte string, length given by next byte
	 * 0x43 : long byte string, length given by next 2 bytes
	 * 0x44-0x47 : reserved?
	 * 0x48-0x4F : integer and floating-point numbers
	 *   of lower 3 bits abc,
	 *   a indicates type: signed int (1) or float (0)
	 *   b c indicate size: 8-bit (0), 16-bit (1), 32-bit (2), or 64-bit (4)
	 *   
	 *   8-bit float is probably not useful; so 0x08 may be put to another use, maybe to mean NaN?
	 */
	
	public static final byte SE_SHORTSTRING  = 0x42;
	public static final byte SE_MEDIUMSTRING = 0x43;
	public static final byte RE_BLOBSHA1     = 0x44;
	public static final byte RE_OBJECTSHA1   = 0x45;
	public static final byte BE_TRUE    = 0x46;
	public static final byte BE_FALSE   = 0x47;
	public static final byte SE_DEFAULT = 0x48;
	public static final byte NE_FLOAT16 = 0x49;
	public static final byte NE_FLOAT32 = 0x4A;
	public static final byte NE_FLOAT64 = 0x4B;
	public static final byte NE_INT8    = 0x4C;
	public static final byte NE_INT16   = 0x4D;
	public static final byte NE_INT32   = 0x4E;
	public static final byte NE_INT64   = 0x4F;
	public static final byte NE_INT6    = (byte)0xFF;
	
	public static final byte encodeSmallInt( int v ) {
		assert fitsIn6Bits(v);
		return (byte)v; 
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
	
	public static final OpcodeDefinition[] STANDARD_OPS = new OpcodeDefinition[0x50];
	static {
		STANDARD_OPS[SE_SHORTSTRING ] = PushShortString.INSTANCE;
		STANDARD_OPS[SE_MEDIUMSTRING] = PushMediumString.INSTANCE;
		STANDARD_OPS[RE_BLOBSHA1    ] = PushSHA1BlobReference.INSTANCE;
		STANDARD_OPS[RE_OBJECTSHA1  ] = PushSHA1ObjectReference.INSTANCE;
		STANDARD_OPS[BE_TRUE        ] = PushTrue.INSTANCE;
		STANDARD_OPS[BE_FALSE       ] = PushFalse.INSTANCE;
		STANDARD_OPS[SE_DEFAULT     ] = PushDefault.INSTANCE;
		STANDARD_OPS[RE_OBJECTSHA1  ] = PushSHA1ObjectReference.INSTANCE;
		STANDARD_OPS[NE_FLOAT16     ] = PushFloat16.INSTANCE;
		STANDARD_OPS[NE_FLOAT32     ] = PushFloat32.INSTANCE;
		STANDARD_OPS[NE_FLOAT64     ] = PushFloat64.INSTANCE;
		STANDARD_OPS[NE_INT8        ] = PushInt8.INSTANCE;
		STANDARD_OPS[NE_INT16       ] = PushInt16.INSTANCE;
		STANDARD_OPS[NE_INT32       ] = PushInt32.INSTANCE;
		STANDARD_OPS[NE_INT64       ] = PushInt64.INSTANCE;
	}
	
	public static final void writeInt6( byte val, OutputStream os ) throws IOException {
		os.write( encodeSmallInt(val) );
	}
	public static final void writeInt8( byte val, OutputStream os ) throws IOException {
		os.write( NE_INT8 );
		os.write( val );
	}
	public static final void writeInt16( short val, OutputStream os ) throws IOException {
		os.write( NE_INT16 );
		NumberEncoding.writeInt16( val, os );
	}
	public static final void writeInt32( int val, OutputStream os ) throws IOException {
		os.write( NE_INT32 );
		NumberEncoding.writeInt32( val, os );
	}
	public static final void writeInt64( long val, OutputStream os ) throws IOException {
		os.write( NE_INT64 );
		NumberEncoding.writeInt64( val, os );
	}
	public static final void writeFloat16( float val, OutputStream os ) throws IOException {
		os.write( NE_FLOAT16 );
		NumberEncoding.writeInt16( (short)Float16.floatToShortBits(val), os );
	}
	public static final void writeFloat32( float val, OutputStream os ) throws IOException {
		os.write( NE_FLOAT32 );
		NumberEncoding.writeInt32( Float.floatToIntBits(val), os );
	}
	public static final void writeFloat64( double val, OutputStream os ) throws IOException {
		os.write( NE_FLOAT64 );
		NumberEncoding.writeInt64( Double.doubleToLongBits(val), os );
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
			float squashed16 = Float16.shortBitsToFloat( Float16.floatToShortBits( fVal ) );
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
			os.write( SE_MEDIUMSTRING );
			NumberEncoding.writeInt16( (short)data.length, os );
			os.write( data );
		} else {
			throw new UnsupportedOperationException("Byte string to be encoded is too long (>65535, "+data.length+" to be exact)");
		}
	}
	
	public static void writeSha1ObjectReference( SHA1ObjectReference sr, OutputStream os ) throws IOException {
		os.write( sr.sha1IdentifiesSerialization() ? RE_OBJECTSHA1 : RE_BLOBSHA1 );
		os.write( sr.getSha1() );
	}
	
	public static void writeSha1ObjectReference( HasURI ref, OutputStream os ) throws IOException {
		try {
			writeSha1ObjectReference( SHA1ObjectReference.parse(ref.getUri()), os );
		} catch( InvalidEncoding e ) {
			throw new UnsupportedOperationException(ref.getUri() + " can't be encoded as an SHA1 object/blob reference", e);
		}
	}
	
	public static void writeValue( Object thing, OutputStream os ) throws IOException {
		if( thing instanceof byte[] ) {
			writeByteString( (byte[])thing, os );
		} else if( thing instanceof Number ) {
			writeNumberCompact( (Number)thing, os );
		} else if( Boolean.TRUE.equals(thing) ) {
			os.write(BE_TRUE);
		} else if( Boolean.FALSE.equals(thing) ) {
			os.write(BE_FALSE);
		} else if( Default.INSTANCE.equals(thing) ) {
			os.write(SE_DEFAULT);
		} else if( thing instanceof SHA1ObjectReference ) {
			writeSha1ObjectReference( (SHA1ObjectReference)thing, os );
		} else {
			throw new UnsupportedOperationException("Don't know how to encode "+thing.getClass());
		}
	}
}
