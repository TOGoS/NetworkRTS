package togos.networkrts.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class ResourceHandlePool
{
	protected final WeakHashMap<SoftResourceHandle<?>,WeakReference<SoftResourceHandle<?>>> refs = new WeakHashMap<SoftResourceHandle<?>,WeakReference<SoftResourceHandle<?>>>();
	
	public synchronized <T> SoftResourceHandle<T> intern( SoftResourceHandle<T> link ) {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		WeakReference<SoftResourceHandle<T>> internedRef = (WeakReference<SoftResourceHandle<T>>)(WeakReference)refs.get(link);
		if( internedRef != null ) {
			// Might disappear if collection occurs ~right here~
			SoftResourceHandle<T> interned = internedRef.get();
			if( interned != null ) return interned;
		}
		refs.put(link, new WeakReference<SoftResourceHandle<?>>(link));
		return link;
	}
	
	public <T> SoftResourceHandle<T> get( String urn ) {
		return intern(new SoftResourceHandle<T>(urn));
	}
}
