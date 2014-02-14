package togos.networkrts.experimental.qt2drender;

import java.awt.image.BufferedImage;

public class Blackifier
{
	static float dist( float x0, float y0, float x1, float y1 ) {
		float dx = x1-x0;
		float dy = y1-y0;
		return (float)Math.sqrt(dx*dx + dy*dy);
	}

	static int clampByte( int v ) {
		return v < 0 ? 0 : v > 255 ? 255 : v;
	}
	
	static int clampByte( float v ) {
		return v < 0 ? 0 : v > 255 ? 255 : (int)v;
	}
	
	static int color( float a, float r, float g, float b ) {
		return
			(clampByte(a) << 24) |
			(clampByte(r) << 16) |
			(clampByte(g) <<  8) |
			(clampByte(b)      );
	}
	
	static int component( int color, int shift ) {
		return (color >> shift) & 0xFF;
	}
	
	static int blacken( int color, float brightness, float visibility ) {
		int originalAlpha = component(color, 24);
		return color(
			originalAlpha + (1-visibility)*(255-originalAlpha),
			component(color, 16)*brightness*visibility,
			component(color,  8)*brightness*visibility,
			component(color,  0)*brightness*visibility
		);
	}
	
	public static BufferedImage shade( BufferedImage img, float brightness, float v0, float v1, float v2, float v3 ) {
		int w = img.getWidth(), h = img.getHeight();
		int[] buf = img.getRGB(0, 0, w, h, new int[w*h], 0, w);
		boolean isCompletelyOpaque = true;
		for( int j=w*h-1, y=h-1; y>=0; --y ) for( int x=w-1; x>=0; --x, --j ) {
			float cx = x+0.5f;
			float cy = y+0.5f;
			/*
			float d0 = dist(0,0,cx,cy);
			float d1 = dist(w,0,cx,cy);
			float d2 = dist(0,h,cx,cy);
			float d3 = dist(w,h,cx,cy);
			float value = (m0/d0 + m1/d1 + m2/d2 + m3/d3) / (1/d0 + 1/d1 + 1/d2 + 1/d3);
			*/
			
			float topValue    = (v0/cx + v1/(w-cx)) / (1/cx + 1/(w-cx));
			float bottomValue = (v2/cx + v3/(w-cx)) / (1/cx + 1/(w-cx));
			float value = (topValue/cy + bottomValue/(h-cy)) / (1/cy + 1/(h-cy));
			value = value * value;
			
			buf[j] = blacken( buf[j], brightness, value );
			if( (buf[j] & 0xFF000000) != 0xFF000000 ) isCompletelyOpaque = false;
		}
		BufferedImage res = new BufferedImage( w, h, isCompletelyOpaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB );
		res.setRGB( 0, 0, w, h, buf, 0, w );
		return res;
	}
	
	public static BufferedImage makeShadeOverlay( int size, float v0, float v1, float v2, float v3 ) {
		return shade( new BufferedImage( size, size, BufferedImage.TYPE_INT_ARGB ), 1f, v0, v1, v2, v3 );
	}
}
