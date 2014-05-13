package togos.networkrts.experimental.hdr64;

public class HDR64Util
{
	static final int HDR_COMPONENT_MASK = (1<<20)-1;

	public static final int hdrComponent( long hdr, int shift ) {
		return (int)(hdr >> shift) & HDR_COMPONENT_MASK;
	}

	public static final int clampToByte( int v ) {
		return v < 0 ? 0 : v > 255 ? 255 : v;
	}
	
	// Alpha is for now not handled because
	// supporting it (in the upper 4 bits)
	// screws up the ability to sum = a + b

	public static long hdr( float a, float r, float g, float b ) {
		return
			//((long)Math.round(a *  15) << 60) |
			((long)Math.round(r * 255) << 40) |
			((long)Math.round(g * 255) << 20) |
			((long)Math.round(b * 255) <<  0);
	}

	public static long intToHdr( int argb, int shift ) {
		return
			//((long)(((argb >> 28)&0x0F) << 60)) |
			((long)(((argb >> 16)&0xFF) << shift) << 40) |
			((long)(((argb >>  8)&0xFF) << shift) << 20) |
			((long)(((argb >>  0)&0xFF) << shift) <<  0);
	}
	
	public static int hdrToInt( long hdr, int shift ) {
		//int alpha4 = (int)(hdr >> 60) & 0xF;
		
		return
			//(alpha4 << 28) | (alpha4 << 24) |
			(clampToByte(hdrComponent(hdr,40) >> shift) << 16) |
			(clampToByte(hdrComponent(hdr,20) >> shift) <<  8) |
			(clampToByte(hdrComponent(hdr, 0) >> shift) <<  0);
	}
	
	protected static long componentMask( int bitsPerComponent ) {
		long cmask = (1<<bitsPerComponent)-1;
		
		return
			(cmask << 40) |
			(cmask << 20) |
			(cmask <<  0);
	}

	public static long shiftDown( long hdr, int shift ) {
		return (hdr >> shift) & componentMask(20-shift);
	}

	public static void fill( HDR64Buffer img, long v ) {
		for( int i=img.height*img.width-1; i>=0; --i ) img.data[i] = v;
	}

	public static void fillRect( HDR64Buffer img, int x, int y, int w, int h, long v, long oldFac ) {
		for( int dy=y+h-1; dy>=y; --dy ) {
			if( dy < 0 || dy >= img.height ) continue;
			for( int dx=x+w-1, off=dy*img.width+dx; dx>=x; --dx, --off ) {
				if( dx < 0 || dx >= img.width ) continue;
				img.data[off] = oldFac*img.data[off] + v;
			}
		}
	}

	public static void add( long[] src, long[] dest ) {
		for( int i=src.length-1; i>=0; --i ) dest[i] += src[i];
	}

}
