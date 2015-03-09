package togos.blob;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import togos.blob.util.BlobUtil;

public class SimpleByteChunk implements ByteChunk
{
	public static final SimpleByteChunk EMPTY = new SimpleByteChunk(new byte[0],0,0);
	
	/** @return a new SimpleByteChunk referencing a new buffer. */
	public static SimpleByteChunk copyOf( byte[] buf, int offset, int size ) {
		byte[] bu2 = new byte[size];
		SimpleByteChunk sbc = new SimpleByteChunk( bu2, 0, size );
		for( size = size-1; size >= 0; --size ) {
			bu2[size] = buf[offset+size];
		}
		return sbc;
	}
	
	/** @return a new SimpleByteChunk referencing a new buffer. */
	public static SimpleByteChunk copyOf( ByteChunk c ) {
		return copyOf( c.getBuffer(), c.getOffset(), BlobUtil.toInt(c.getSize()) );
	}
	
	public final byte[] buffer;
	public final int offset;
	public final int size;
	
	public SimpleByteChunk( byte[] buf, int offset, int size ) {
		this.buffer = buf;
		this.offset = offset;
		this.size = size;
	}
	
	public SimpleByteChunk( byte[] buf ) {
		this( buf, 0, buf.length );
	}
	
	public byte[] getBuffer() { return buffer; }
	public int getOffset() { return offset; }
	public long getSize() { return size; }
	
	public int hashCode() {
		return BlobUtil.hashCode(buffer, offset, size);
	}
	
	public boolean equals( Object o ) {
		if( o instanceof ByteChunk ) return BlobUtil.equals( this, (ByteChunk)o );
		return false;
	}
	
	@Override public InputStream openInputStream() throws IOException {
		return new ByteArrayInputStream(buffer, offset, size);
	}
	
	public String toString() {
		// TODO: when not valid UTF-8 text, do it different
		return BlobUtil.string(buffer, offset, size);
	}
	
}
