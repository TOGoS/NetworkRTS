package togos.networkrts.experimental.qt2drender.demo;

import java.awt.image.BufferedImage;
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
		
		if( "transparent:16x16".equals(name) ) {
			h = new ImageHandle(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
			for( int y=0; y<16; ++y ) for( int x=0; x<16; ++x ) h.image.setRGB(x,y,0x00000000);
		} else {
			try {
				h = new ImageHandle(ImageIO.read(new File(name)));
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		}
		handles.put(name, h);
		return h;
	}
	
	public synchronized ImageHandle getShaded( String name, float brightness, float v0, float v1, float v2, float v3 ) {
		String longName = name + ";shaded:"+brightness+","+v0+","+v1+","+v2+","+v3;
		ImageHandle ih = handles.get(longName);
		if( ih != null ) return ih;
		
		ih = get(name);
		ih = new ImageHandle( Blackifier.shade( ih.image, brightness, v0, v1, v2, v3 ) );
		
		handles.put(longName, ih);
		return ih;
	}
}
