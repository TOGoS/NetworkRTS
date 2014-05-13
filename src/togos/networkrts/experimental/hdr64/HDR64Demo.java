package togos.networkrts.experimental.hdr64;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import javax.imageio.ImageIO;

import togos.networkrts.ui.ImageCanvas;

public class HDR64Demo
{
	enum DrawMode {
		REPLACE,
		ADD
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
	
	public static void bleed( HDR64Buffer src, HDR64Buffer dest ) {
		for( int x=0; x<src.width; ++x ) {
			long val = 0;
			for( int y=0; y<src.height; ++y ) {
				int idx=x+src.width*y;
				val += src.data[idx];
				val = HDR64Util.shiftDown(val, 1);
				dest.data[idx] += val;
			}
			for( int y=src.height-1; y>=0; --y ) {
				int idx=x+src.width*y;
				val += src.data[idx];
				val = HDR64Util.shiftDown(val, 1);
				dest.data[idx] += val;
			}
		}
	}
	
	public static void radBleed( HDR64Buffer src, HDR64Buffer dest ) {
		for( int y=0; y<src.height; ++y ) {
			long val = 0;
			for( int x=0, idx=src.width*y; x<src.width; ++x, ++idx ) {
				val += HDR64Util.shiftDown(src.data[idx], 4);
			}
			val = HDR64Util.shiftDown(val, 7);
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
		leCanv.setBackground(Color.BLACK);
		leCanv.setPreferredSize(new Dimension(640,480));
		f.add(leCanv);
		f.pack();
		f.setVisible(true);
		
		class Sprite {
			//long color = 0;
			float x = 0;
			float y = 0;
			float dx = 1;
			float dy = 1;
			HDR64Drawable icon;
			//float w = 3;
			//float h = 3;
		}
		
		BufferedImage jetManImage = ImageIO.read(new File("tile-images/JetMan/JetUp.png"));
		HDR64Drawable jetManDrawable = HDR64IO.toHdr64Drawable(jetManImage, 0);
		HDR64Drawable jetManBrightDrawable = HDR64IO.toHdr64Drawable(jetManImage, 8);
		
		Random r = new Random();
		Sprite[] sprites = new Sprite[100];
		for( int i=0; i<sprites.length; ++i ) {
			sprites[i] = new Sprite();
			sprites[i].x = r.nextInt(300);
			sprites[i].y = r.nextInt(300);
			sprites[i].dy = r.nextFloat()*10;
			sprites[i].dx = r.nextFloat()*10;
			//sprites[i].color = HDR64Util.hdr(r.nextFloat()*r.nextFloat()*2, r.nextFloat(), 0.5f);
			sprites[i].icon = jetManDrawable;
			while( r.nextBoolean() ) {
				sprites[i].dx *= 0.5f;
				sprites[i].dy *= 0.5f;
				//sprites[i].w *= 1.2f;
				//sprites[i].h *= 1.2f;
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
			
			HDR64Util.fill( accBuf, 0 );
			for( int i=0; i<iters; ++i ) {
				HDR64Util.fill( drawBuf, 0 );
				for( Sprite s : sprites ) {
					s.x += 0.05 * s.dx * tickLen / iters;
					s.y += 0.05 * s.dy * tickLen / iters;
					boolean bounce = false;
					if( s.x < 0 && s.dx < 0 ) { s.dx = -s.dx; bounce = true; }
					if( s.y < 0 && s.dy < 0 ) { s.dy = -s.dy; bounce = true; }
					if( s.x > w && s.dx > 0 ) { s.dx = -s.dx; bounce = true; }
					if( s.y > h && s.dy > 0 ) { s.dy = -s.dy; bounce = true; }
					
					s.dy += 0.1;
					
					//long color = bounce ? HDR64Util.hdr(500,100,10) : s.color;
					
					HDR64Drawable drawable = bounce ? jetManBrightDrawable : s.icon;
					drawable.draw(drawBuf, (int)s.x-8, (int)s.y-8, 0, 0, drawBuf.width, drawBuf.height);
					//HDR64Util.fillRect( drawBuf, (int)s.x, (int)s.y, (int)s.w, (int)s.h, color, 0);
				}
				HDR64Util.add( drawBuf.data, accBuf.data );
			}
			
			bleed( accBuf, drawBuf );
			radBleed( drawBuf, accBuf );
			
			bufImg = HDR64IO.toBufferedImage(accBuf, iterPow, bufImg, BufferedImage.TYPE_INT_RGB);
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
