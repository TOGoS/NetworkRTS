package togos.networkrts.experimental.rocopro.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import togos.networkrts.experimental.rocopro.RCMessage;

public class SimpleMessageCodec
{
	static class MessageCodecException extends Exception {
		private static final long serialVersionUID = 1L;
		
		public MessageCodecException( Exception cause ) {
			super(cause);
		}
	}
	
	public static byte[] encode( RCMessage m ) throws MessageCodecException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(m);
			oos.close();
		} catch( IOException e ) {
			// Shouldn't happen unless something's not serializable
			throw new MessageCodecException(e);
		}
		return baos.toByteArray();
	}
	
	public RCMessage decode( byte[] buf ) throws MessageCodecException {
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		try {
			ObjectInputStream ois = new ObjectInputStream(bais);
			return (RCMessage)ois.readObject();
		} catch( ClassNotFoundException e ) {
			throw new MessageCodecException(e);
		} catch( IOException e ) {
			throw new MessageCodecException(e);
		}
	}
}
