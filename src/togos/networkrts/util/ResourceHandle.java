package togos.networkrts.util;

import java.lang.ref.SoftReference;

public class ResourceHandle<T>
{
	protected final String uri;
	public ResourceHandle( String uri ) {
		this.uri = uri;
	}
	
	protected boolean beingPopulated;
	protected boolean hadUnrecoverablePopulationError;
	protected volatile SoftReference<T> ref;
	
	public String getUri() {
		return uri;
	}
	
	public synchronized boolean lockForPopulation() {
		if( beingPopulated ) return false;
		if( hadUnrecoverablePopulationError ) return false;
		if( ref != null && ref.get() != null ) return false;
		
		beingPopulated = true;
		return true;
	}
	
	public T getValue() {
		SoftReference<T> ref = this.ref;
		return ref == null ? null : ref.get();
	}
	
	public synchronized void setPopulationError() {
		beingPopulated = false;
		hadUnrecoverablePopulationError = true;
		ref = null;
	}
	
	public synchronized void setValue( T v ) {
		beingPopulated = false;
		hadUnrecoverablePopulationError = false;
		ref = new SoftReference(v);
	}
	
	@Override public int hashCode() {
		return uri.hashCode();
	}
	@Override public boolean equals(Object o) {
		return o instanceof ResourceHandle && uri.equals(((ResourceHandle)o).getUri());
	}
}
