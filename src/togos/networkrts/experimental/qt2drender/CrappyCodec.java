package togos.networkrts.experimental.qt2drender;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class CrappyCodec
{
	public static byte[] encode(Serializable obj) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.close();
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		return baos.toByteArray();
	}
	
	public static int encode(Serializable obj, byte[] dest, int offset, int maxLength) {
		byte[] dat = encode(obj);
		if( dat.length > maxLength ) return -1;
		
		for( int i=0; i<dat.length; ++i ) {
			dest[offset++] = dat[i];
		}
		return dat.length;
	}
	
	public static int encode(Serializable obj, byte[] dest) {
		return encode(obj, dest, 0, dest.length);
	}
	
	public static <T> T decode(byte[] data, int offset, int length, Class<T> c )
		throws ClassNotFoundException
	{
		Object obj;
		try {
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data, offset, length));
			obj = ois.readObject();
			ois.close();
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		return c.cast(obj);
	}
}
