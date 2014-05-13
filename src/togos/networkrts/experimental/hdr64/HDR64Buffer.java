package togos.networkrts.experimental.hdr64;

/**
 * Represents an image using 64-bit integers
 * each component (R,G,B) is up to 20 bits.
 * As long as upper bits are zero, certain operations
 * (adding, multiplying or dividing by powers of 2)
 * can be done using a single operation to the entire
 * pixel.
 */
public class HDR64Buffer implements HDR64Drawable
{
	final int width, height;
	final long[] data;
	
	public HDR64Buffer( int w, int h ) {
		this.width = w;
		this.height = h;
		this.data = new long[w*h];
	}
	
	public static HDR64Buffer get( HDR64Buffer buf, int w, int h ) {
		if( buf == null || buf.width != w || buf.height != h ) {
			return new HDR64Buffer(w, h);
		} else {
			return buf;
		}
	}
	
	@Override public void draw(
		HDR64Buffer dest, int x, int y,
		int clipLeft, int clipTop, int clipRight, int clipBottom
	) {
		assert clipLeft >= 0;
		assert clipTop >= 0;
		assert clipRight <= dest.width;
		assert clipBottom <= dest.height;
		
		// TODO: more efficiently!
		int skipY = clipTop  > y ? clipTop  - y : 0;
		int skipX = clipLeft > x ? clipLeft - x : 0;
		for( int sy=skipY, dy=y+skipY; sy<height && dy<clipBottom; ++sy, ++dy ) {
			for(
				int sx=skipX, dx=x+skipX, si=width*sy+sx, di=dest.width*dy+dx;
				sx<width && dx<clipRight;
				++sx, ++dx, ++si, ++di
			) {
				long v = data[si];
				// TODO: Handle alpha better!
				if( (v & 0x8000000000000000l) == 0x8000000000000000l ) {
					dest.data[di] = v;
				}
			}
		}
	}
}
