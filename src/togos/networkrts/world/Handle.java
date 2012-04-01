package togos.networkrts.world;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import togos.blob.ByteChunk;
import togos.blob.SimpleByteChunk;

public class Handle
{
	protected static WeakHashMap instances = new WeakHashMap();
	public static Handle getInstance( ByteChunk id ) {
		Handle h = new Handle(id);
		WeakReference i;
		Handle j;
		synchronized( instances ) {
			i = (WeakReference)instances.get(h);
			if( i != null && (j = (Handle)i.get()) != null ) {
				return j;
			} else {
				instances.put( h, i = new WeakReference(h) );
				return h;
			}
		}
	}
	
	public static Handle getInstance( byte[] buf, int offset, int size ) {
		return getInstance( new SimpleByteChunk(buf, offset, size) );
	}
	
	final ByteChunk id;
	SoftReference valueReference;
	
	protected Handle( ByteChunk id ) {
		this.id = id;
	}
	
	protected Handle( ByteChunk id, Object value ) {
		this( id );
		this.valueReference = new SoftReference(value);
	}
	
	public int hashCode() { return id.hashCode(); }
	
	public boolean equals( Object something ) {
		return something instanceof Handle && id.equals(((Handle)something).id);
	}
}
