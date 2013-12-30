package togos.networkrts.experimental.qt2drender.demo;

import java.util.HashMap;

import togos.networkrts.experimental.qt2drender.ImageHandle;

public class ImageHandlePool
{
	HashMap<String,ImageHandle> handles = new HashMap<String,ImageHandle>();
	
	public synchronized ImageHandle get( String uri ) {
		ImageHandle h = handles.get(uri);
		if( h != null ) return h;
		
		handles.put(uri, new ImageHandle(uri));
		return h;
	}
}
