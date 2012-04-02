package togos.networkrts.world;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import togos.blob.ByteChunk;
import togos.blob.SimpleByteChunk;
import togos.networkrts.resource.IDs;

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
	
	public static final Handle NULL_HANDLE = getInstance( IDs.NULL_ID );
	
	public static Handle getInstance( ByteChunk id, Object value, boolean harden ) {
		Handle h = getInstance(id);
		h.setValue(value, harden);
		return h;
	}
	
	/**
	 * Make handles without having to actually store objects.
	 * Will not be very useful for communication, but can be used for
	 * building structures to test with.
	 */
	public static Handle getHardAnonymousInstance( Object value ) {
		Handle h = getInstance(IDs.random());
		h.setValue(value, true);
		return h;
	}
	
	public static Handle getInstance( byte[] buf, int offset, int size ) {
		return getInstance( new SimpleByteChunk(buf, offset, size) );
	}
	
	public final ByteChunk id;
	SoftReference valueReference;
	protected Object value; 
	
	protected Handle( ByteChunk id ) {
		this.id = id;
	}
	
	protected Handle( ByteChunk id, Object value ) {
		this( id );
		this.valueReference = new SoftReference(value);
	}
	
	public Object getValue() {
		return valueReference == null ? null : valueReference.get();
	}
	
	public void setValue( Object value, boolean harden ) {
		valueReference = new SoftReference(value);
		if( harden ) this.value = value;
	}
	
	public int hashCode() { return id.hashCode(); }
	
	public boolean equals( Object something ) {
		return something instanceof Handle && id.equals(((Handle)something).id);
	}
}
