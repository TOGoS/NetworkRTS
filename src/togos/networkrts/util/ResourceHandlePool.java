package togos.networkrts.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class ResourceHandlePool
{
	protected final WeakHashMap<ResourceHandle<?>,WeakReference<ResourceHandle<?>>> refs = new WeakHashMap<ResourceHandle<?>,WeakReference<ResourceHandle<?>>>();
	
	public synchronized <T> ResourceHandle<T> intern( ResourceHandle<T> link ) {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		WeakReference<ResourceHandle<T>> internedRef = (WeakReference<ResourceHandle<T>>)(WeakReference)refs.get(link);
		if( internedRef != null ) {
			// Might disappear if collection occurs ~right here~
			ResourceHandle<T> interned = internedRef.get();
			if( interned != null ) return interned;
		}
		refs.put(link, new WeakReference<ResourceHandle<?>>(link));
		return link;
	}
	
	public <T> ResourceHandle<T> get( String urn ) {
		return intern(new ResourceHandle<T>(urn));
	}
}
