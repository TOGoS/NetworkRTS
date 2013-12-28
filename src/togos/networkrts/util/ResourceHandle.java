package togos.networkrts.util;

import java.lang.ref.SoftReference;

public class ResourceHandle<T>
{
	protected final String uri;
	public ResourceHandle( String uri ) {
		this.uri = uri;
	}
	
	protected boolean beingPopulated;
	protected Exception error;
	protected volatile SoftReference<T> ref;
	
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
	
	public <E extends Throwable> T getValue( Getter<T> populator ) {
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
			System.err.println("Error populating "+uri);
			e.printStackTrace();
			setError(e);
		}
		
		return value;
	}
	
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
