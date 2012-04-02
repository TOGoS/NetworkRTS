package togos.networkrts.resource;

import java.util.Random;

import togos.blob.ByteChunk;
import togos.blob.SimpleByteChunk;

public class IDs
{
	public static final SimpleByteChunk NULL_ID = new SimpleByteChunk( new byte[20], 0, 20 );
	
	public static ByteChunk random() {
		byte[] name = new byte[20];
		new Random().nextBytes(name);
		return new SimpleByteChunk(name);
	}
}
