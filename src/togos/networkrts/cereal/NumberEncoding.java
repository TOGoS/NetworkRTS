package togos.networkrts.cereal;

import java.io.IOException;
import java.io.OutputStream;

import togos.networkrts.util.Float16;

public class NumberEncoding {

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

}
