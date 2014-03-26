package togos.networkrts.cereal;

import java.io.IOException;
import java.io.OutputStream;

import togos.networkrts.util.Float16;

public class NumberEncoding
{
	public static float readFloat16( byte[] data, int offset ) {
		return Float16.shortBitsToFloat(readInt16(data, offset));
	}

	public static float readFloat32( byte[] data, int offset ) {
		return Float.intBitsToFloat(readInt32(data, offset));
	}

	public static double readFloat64( byte[] data, int offset ) {
		return Double.longBitsToDouble(readInt64(data, offset));
	}

	public static short readInt16( byte[] data, int offset ) {
		return (short)(
			((data[offset+0]&0xFF) << 8) |
			((data[offset+1]&0xFF) << 0)
		);
	}

	public static int readInt32( byte[] data, int offset ) {
		return
			((data[offset+0]&0xFF) << 24) |
			((data[offset+1]&0xFF) << 16) |
			((data[offset+2]&0xFF) <<  8) |
			((data[offset+3]&0xFF) <<  0);
	}

	public static long readInt64( byte[] data, int offset ) {
		return
			((long)(data[offset+0]&0xFF) << 56) |
			((long)(data[offset+1]&0xFF) << 48) |
			((long)(data[offset+2]&0xFF) << 40) |
			((long)(data[offset+3]&0xFF) << 32) |
			((long)(data[offset+4]&0xFF) << 24) |
			((long)(data[offset+5]&0xFF) << 16) |
			((long)(data[offset+6]&0xFF) <<  8) |
			((long)(data[offset+7]&0xFF) <<  0);
	}
	
	/*
	public static long readBase128( byte[] data, int offset ) {
		byte octet = data[offset];
		long value = (((octet & 0x40) == 0x40 ? -1 : 0) << 7) | (octet & 0x7F);
		int read;
		for( read=1; (octet & 0x80) == 0x80; ++read ) {
			if( read == 8 ) {
				throw new RuntimeException("Decoded value would not fit in 56 bits");
			}
			octet = data[offset+read];
			value = (value << 7) | (octet & 0x7F);
		}
		
		return ((long)read << 56) | (value & 0x00FFFFFFFFFFFFFFl);
	}
	*/
	
	/**
	 * Returns a long where the upper 8 bits indicate the number of bytes read,
	 * and the lower 56 indicate the number read,
	 * It will throw an error if a number is too long to be represented in 56 bits.
	 * 
	 * The format is https://en.wikipedia.org/wiki/Variable-length_quantity
	 */
	public static long readUnsignedBase128( byte[] data, int offset ) throws InvalidEncoding {
		byte octet = data[offset];
		long value = (octet & 0x7F);
		int read;
		for( read=1; (octet & 0x80) == 0x80; ++read ) {
			if( read == 8 ) {
				throw new InvalidEncoding("Decoded value would not fit in 56 bits");
			}
			if( offset+read >= data.length ) {
				throw new InvalidEncoding("Base128-encoded number terminated by end of input");
			}
			octet = data[offset+read];
			value = (value << 7) | (octet & 0x7F);
		}
		
		return ((long)read << 56) | (value & 0x00FFFFFFFFFFFFFFl);
	}
	
	public static void writeInt16( long v, OutputStream os ) throws IOException {
		byte[] buf = new byte[2];
		buf[0] = (byte)(v>> 8);
		buf[1] = (byte)(v>> 0);
		os.write(buf);
	}

	public static void writeInt32( long v, OutputStream os ) throws IOException {
		byte[] buf = new byte[4];
		buf[0] = (byte)(v>>24);
		buf[1] = (byte)(v>>16);
		buf[2] = (byte)(v>> 8);
		buf[3] = (byte)(v>> 0);
		os.write(buf);
	}

	public static void writeInt64( long v, OutputStream os ) throws IOException {
		byte[] buf = new byte[8];
		buf[0] = (byte)(v>>56);
		buf[1] = (byte)(v>>48);
		buf[2] = (byte)(v>>40);
		buf[3] = (byte)(v>>32);
		buf[4] = (byte)(v>>24);
		buf[5] = (byte)(v>>16);
		buf[6] = (byte)(v>> 8);
		buf[7] = (byte)(v>> 0);
		os.write(buf);
	}
	
	/*
	public static void writeBase128( long v, OutputStream os ) throws IOException {
		byte[] buf = new byte[8];
		int i=8;
		do {
			--i;
			buf[i] = (byte)((i == 7 ? 0 : 0x80) | ((int)v & 0x7F));
			v >>= 7;
		} while( v < -1 || v > 0 );
		os.write( buf, i, 8-i );
	}
	*/
	
	public static void writeUnsignedBase128( long v, OutputStream os ) throws IOException {
		if( v < 0 ) throw new UnsupportedOperationException("Can't encode "+v+" as unsigned base-128 because it is negative!");
		byte[] buf = new byte[8];
		int i=8;
		do {
			if( --i < 0 ) throw new UnsupportedOperationException("Number too big to fit in 8 base128 digits: "+v);
			buf[i] = (byte)((i == 7 ? 0 : 0x80) | ((int)v & 0x7F));
			v >>= 7;
		} while( v > 0 );
		os.write( buf, i, 8-i );
	}

	private NumberEncoding() { }
}
