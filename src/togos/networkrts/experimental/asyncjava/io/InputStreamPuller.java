package togos.networkrts.experimental.asyncjava.io;

import java.io.IOException;
import java.io.InputStream;

import togos.networkrts.experimental.asyncjava.Puller;

public class InputStreamPuller implements Puller<byte[]>
{
	protected final InputStream is;
	protected final byte[] buffer = new byte[65536];
	Thread t;
	
	public InputStreamPuller( InputStream is ) {
		this.is = is;
		
	}
	
	@Override public byte[] pull() throws IOException {
		t = Thread.currentThread();
		int size = is.read( buffer );
		if( size <= 0 ) return null;
		byte[] data = new byte[size];
		for( int i=size-1; i>=0; --i ) data[i] = buffer[i];
		return data;
	}
	
	@Override
	public void stop() {
		try {
			is.close();
		} catch( Exception e ) {
		}
	}
}
