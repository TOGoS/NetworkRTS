package togos.networkrts.experimental.qt2drender.demo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import togos.networkrts.experimental.qt2drender.ImageHandle;
import togos.networkrts.experimental.qt2drender.Blackifier;

public class ImageHandleCache
{
	HashMap<String,ImageHandle> handles = new HashMap<String,ImageHandle>();
	
	public synchronized ImageHandle get( String name ) {
		ImageHandle h = handles.get(name);
		if( h != null ) return h;
		
		try {
			h = new ImageHandle(ImageIO.read(new File(name)));
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		handles.put(name, h);
		return h;
	}
	
	public synchronized ImageHandle getShaded( String name, float s0, float s1, float s2, float s3 ) {
		String longName = name + ";shaded:"+s0+","+s1+","+s2+","+s3;
		ImageHandle ih = handles.get(longName);
		if( ih != null ) return ih;
		
		ih = get(name);
		ih = new ImageHandle( Blackifier.shade( ih.image, s0, s1, s2, s3 ) );
		
		handles.put(longName, ih);
		return ih;
	}
}
