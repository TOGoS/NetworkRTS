package togos.networkrts.experimental.hdr64;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

import togos.networkrts.ui.ImageCanvas;

public class HDR64Demo
{
	enum DrawMode {
		REPLACE,
		ADD
	}
	
	public static void add( long[] src, long[] dest ) {
		for( int i=src.length-1; i>=0; --i ) dest[i] += src[i];
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
	
	public void draw( HDR64Buffer src, HDR64Buffer dst, int x, int y, DrawMode mode ) {
		for( int sy=0; sy<src.height; ++sy ) {
			int dy = y+sy;
			if( dy < 0 || dy >= dst.height ) continue;
			for( int sx=0; sx<src.width; ++sx ) {
				int dx = sx+x;
				if( dx < 0 || dx >= dst.width ) continue;
				
				int sIdx = sy*src.width+sx;
				int dIdx = dy*dst.width+dx;
				
				switch( mode ) {
				case ADD:
					dst.data[dIdx] += src.data[sIdx];
					break;
				case REPLACE:
					dst.data[dIdx] = src.data[sIdx];
					break;
				}
			}
		}
	}
	
	static final int HDR_COMPONENT_MASK = (1<<20)-1;

	public static final int hdrComponent( long hdr, int shift ) {
		return (int)(hdr >> shift) & HDR_COMPONENT_MASK;
	}
	
	public static final int clampToByte( int v ) {
		return v < 0 ? 0 : v > 255 ? 255 : v;
	}
	
	public static long hdr( float r, float g, float b ) {
		return
			((long)(r * 255) << 40) |
			((long)(g * 255) << 20) |
			((long)(b * 255) <<  0);
	}
	
	public static long intToHdr( int rgb, int shift ) {
		return
			((long)(((rgb >> 16)&0xFF) << shift) << 40) |
			((long)(((rgb >>  8)&0xFF) << shift) << 20) |
			((long)(((rgb >>  0)&0xFF) << shift) <<  0);
	}
	
	public static int hdrToInt( long hdr, int shift ) {
		return
			0xFF000000 | // TODO: get alpha from hdr
			(clampToByte(hdrComponent(hdr,40) >> shift) << 16) |
			(clampToByte(hdrComponent(hdr,20) >> shift) <<  8) |
			(clampToByte(hdrComponent(hdr, 0) >> shift) <<  0);
	}
	
	public static BufferedImage toBufferedImage( HDR64Buffer img, BufferedImage buf, int shift ) {
		if( buf == null || buf.getWidth() != img.width || buf.getHeight() != img.height || buf.getType() != BufferedImage.TYPE_INT_RGB) {
			buf = new BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB);
		}
		int[] row = new int[img.width];
		for( int y=img.height-1; y>=0; --y ) {
			for( int x=img.width-1, idx=y*img.width+x; x>=0; --x, --idx ) {
				row[x] = hdrToInt(img.data[idx], shift);
			}
			buf.setRGB(0, y, img.width, 1, row, 0, row.length);
		}
		return buf;
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
	
	public static void bleed( HDR64Buffer src, HDR64Buffer dest ) {
		for( int x=0; x<src.width; ++x ) {
			long val = 0;
			for( int y=0; y<src.height; ++y ) {
				int idx=x+src.width*y;
				val += src.data[idx];
				val = shiftDown(val, 1);
				dest.data[idx] += val;
			}
			for( int y=src.height-1; y>=0; --y ) {
				int idx=x+src.width*y;
				val += src.data[idx];
				val = shiftDown(val, 1);
				dest.data[idx] += val;
			}
		}
	}
	
	public static void radBleed( HDR64Buffer src, HDR64Buffer dest ) {
		for( int y=0; y<src.height; ++y ) {
			long val = 0;
			for( int x=0, idx=src.width*y; x<src.width; ++x, ++idx ) {
				val += shiftDown(src.data[idx], 4);
			}
			val = shiftDown(val, 7);
			for( int x=src.width-1, idx=src.width*y+x; x>=0; --x, --idx ) {
				dest.data[idx] = src.data[idx] + val;
			}
		}
	}

	
	public static void main( String[] args ) throws Exception {
		ImageCanvas leCanv = new ImageCanvas();
		Frame f = new Frame("HDR64Demo");
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent wEvt) {
				System.exit(0);
			}
		});
		leCanv.setPreferredSize(new Dimension(640,480));
		f.add(leCanv);
		f.pack();
		f.setVisible(true);
		
		class Sprite {
			long color = 0;
			float x = 0;
			float y = 0;
			float dx = 1;
			float dy = 1;
			float w = 3;
			float h = 3;
		}
		
		Random r = new Random();
		Sprite[] sprites = new Sprite[100];
		for( int i=0; i<sprites.length; ++i ) {
			sprites[i] = new Sprite();
			sprites[i].x = r.nextInt(300);
			sprites[i].y = r.nextInt(300);
			sprites[i].dy = r.nextFloat()*10;
			sprites[i].dx = r.nextFloat()*10;
			sprites[i].color = hdr(r.nextFloat()*r.nextFloat()*2, r.nextFloat(), 0.5f);
			while( r.nextBoolean() ) {
				sprites[i].dx *= 0.5f;
				sprites[i].dy *= 0.5f;
				sprites[i].w *= 1.2f;
				sprites[i].h *= 1.2f;
			}
		}
		
		HDR64Buffer drawBuf = null;
		HDR64Buffer accBuf = null;
		BufferedImage bufImg = null;
		
		long tickLen = 20;
		int iterPow = 4;
		while( true ) {
			long startTime = System.currentTimeMillis(); 
			
			int w = leCanv.getWidth();
			int h = leCanv.getHeight();
			while( w*h > 400*300 ) {
				w >>= 1;
				h >>= 1;
			}
			
			//System.err.println(w+","+h);
			drawBuf = HDR64Buffer.get(drawBuf, w, h);
			accBuf = HDR64Buffer.get(accBuf, w, h);
			
			int iters = 1<<iterPow;
			
			fill( accBuf, 0 );
			for( int i=0; i<iters; ++i ) {
				fill( drawBuf, 0 );
				for( Sprite s : sprites ) {
					s.x += 0.05 * s.dx * tickLen / iters;
					s.y += 0.05 * s.dy * tickLen / iters;
					boolean bounce = false;
					if( s.x < 0 && s.dx < 0 ) { s.dx = -s.dx; bounce = true; }
					if( s.y < 0 && s.dy < 0 ) { s.dy = -s.dy; bounce = true; }
					if( s.x+s.w > w && s.dx > 0 ) { s.dx = -s.dx; bounce = true; }
					if( s.y+s.h > h && s.dy > 0 ) { s.dy = -s.dy; bounce = true; }
					
					s.dy += 0.1;
					
					long color = bounce ? hdr(500,100,10) : s.color;
					fillRect( drawBuf, (int)s.x, (int)s.y, (int)s.w, (int)s.h, color, 0);
				}
				add( drawBuf.data, accBuf.data );
			}
			
			bleed( accBuf, drawBuf );
			radBleed( drawBuf, accBuf );
			
			bufImg = toBufferedImage(accBuf, bufImg, iterPow);
			leCanv.setImage(bufImg);
			
			long targetEndTime = startTime + tickLen;
			long curTime = System.currentTimeMillis();
			long sleepTime = targetEndTime - curTime;
			if( sleepTime > 0 ) {
				Thread.sleep(sleepTime);
				if( sleepTime > tickLen * 2 / 3 ) ++iterPow;
			} else if( iterPow > 0 ) {
				--iterPow;
			}
		}
	}
}
