package togos.networkrts.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class ResourceHandlePool
{
	protected final WeakHashMap<ResourceHandle,WeakReference<ResourceHandle>> refs = new WeakHashMap<ResourceHandle,WeakReference<ResourceHandle>>();
	
	public synchronized ResourceHandle intern( ResourceHandle link ) {
		WeakReference<ResourceHandle> internedRef = refs.get(link);
		if( internedRef != null ) {
			// Might disappear if collection occurs ~right here~
			ResourceHandle interned = internedRef.get();
			if( interned != null ) return interned;
		}
		refs.put(link, new WeakReference<ResourceHandle>(link));
		return link;
	}
	
	public ResourceHandle get( String urn ) {
		return intern(new ResourceHandle(urn));
	}
}