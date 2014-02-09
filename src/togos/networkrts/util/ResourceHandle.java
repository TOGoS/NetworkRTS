package togos.networkrts.util;

import java.io.Serializable;
import java.lang.ref.SoftReference;

public class ResourceHandle<T> implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	protected final String uri;
	public ResourceHandle( String uri ) {
		this.uri = uri;
	}
	public ResourceHandle( String uri, T value ) {
		this(uri);
		assert value != null;
		this.ref = new SoftReference<T>(value);
	}
	
	transient protected boolean beingPopulated;
	transient protected Exception error;
	transient protected volatile SoftReference<T> ref;
	
	public String getUri() {
		return uri;
	}
	
	public synchronized boolean lockForPopulation() {
		if( beingPopulated ) return false;
		if( error != null ) return false;
		if( ref != null && ref.get() != null ) return false;
		
		beingPopulated = true;
		return true;
	}
	
	protected void rethrow( Exception e ) throws ResourceNotFound {
		if( e instanceof ResourceNotFound ) {
			throw (ResourceNotFound)e;
		} else if( e instanceof RuntimeException ) {
			throw (RuntimeException)e;
		} else if( e instanceof InterruptedException ) {
			Thread.currentThread().interrupt();
		} else {
			throw new RuntimeException(e);
		}
	}
	
	public <E extends Throwable> T getValue( Getter<T> populator )
		throws ResourceNotFound
	{
		T value = getValue();
		if( value != null ) return value;
		
		while( !lockForPopulation() ) {
			synchronized( this ) {
				while(beingPopulated) try {
					wait();
				} catch( InterruptedException e ) {
					Thread.currentThread().interrupt();
				}
				value = (ref == null ? null : ref.get());
				// Value may have been lost already
				if( value != null || error != null ) return value;
			}
		}
		try {
			setValue(value = populator.get(uri));
		} catch( Exception e ) {
			setError(e);
			rethrow(e);
		}
		
		return value;
	}
	
	/**
	 * Get the value if it's already populated.
	 * Return null if it hasn't been populated, including if an error occurred while trying to populate.
	 */
	public T getValue() {
		SoftReference<T> ref = this.ref;
		return ref == null ? null : ref.get();
	}
	
	public synchronized void setError( Exception e ) {
		beingPopulated = false;
		error = e;
		ref = null;
		notifyAll();
	}
	
	public synchronized void setValue( T v ) {
		beingPopulated = false;
		error = null;
		ref = new SoftReference<T>(v);
		notifyAll();
	}
	
	@Override public int hashCode() {
		return uri.hashCode();
	}
	@Override public boolean equals(Object o) {
		return o instanceof ResourceHandle && uri.equals(((ResourceHandle<?>)o).getUri());
	}
}
