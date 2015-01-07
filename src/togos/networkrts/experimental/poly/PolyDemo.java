package togos.networkrts.experimental.poly;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import togos.networkrts.ui.ImageCanvas;

public class PolyDemo {
	static class Surface {
		public final int w, h;
		public final short[] r;
		public final short[] g;
		public final short[] b;

		public Surface(int w, int h) {
			this.w = w;
			this.h = h;
			this.r = new short[w * h];
			this.g = new short[w * h];
			this.b = new short[w * h];
		}

		public void fill(short r, short g, short b) {
			for( int i = w * h - 1; i >= 0; --i ) {
				this.r[i] = r;
				this.g[i] = g;
				this.b[i] = b;
			}
		}
		
		public void toArgb(int[] dest, short r, short g, short b, int div) {
			assert dest.length >= w * h;
			for (int i = w * h - 1; i >= 0; --i) {
				short sr = (short) (this.r[i] * r / div);
				short sg = (short) (this.g[i] * r / div);
				short sb = (short) (this.b[i] * r / div);
				if( sr < 0 || sr > 255 ) sr = 255;
				if( sg < 0 || sg > 255 ) sg = 255;
				if( sb < 0 || sb > 255 ) sb = 255;
				dest[i] = 0xFF000000 | (sr << 16) | (sg << 8) | sb;
			}
		}
	}
	
	static class Compositor extends Surface {
		public Compositor(int w, int h) {
			super(w, h);
		}
		
		public void add(Surface s, short r, short g, short b, int div) {
			assert s.w == w;
			assert s.h == h;
			for( int i = w * h - 1; i >= 0; --i ) {
				this.r[i] += (short)(s.r[i] * r / div);
				this.g[i] += (short)(s.g[i] * g / div);
				this.b[i] += (short)(s.b[i] * b / div);
				if( this.r[i] < 0 ) this.r[i] = Short.MAX_VALUE;
				if( this.g[i] < 0 ) this.g[i] = Short.MAX_VALUE;
				if( this.b[i] < 0 ) this.b[i] = Short.MAX_VALUE;
			}
		}
	}

	static class Renderer extends Surface {
		protected float poX, poY, screenDist;
		public float projectedX, projectedY, projectedScale; 
		
		public Renderer(int w, int h) {
			super(w, h);
		}
		
		public void project( float x, float y, float z ) {
			projectedScale = screenDist / (z + screenDist); 
			projectedX = poX + (x-poX)*projectedScale;
			projectedY = poY + (y-poY)*projectedScale;
		}
		
		/**
		 * Indicate the point on the screen where projected x, y will remain constant
		 * as z changes, and the distance from the viewer to the screen. 
		 */
		public void setPerspectiveOrigin( float x, float y, float screenDist ) {
			this.poX = x;
			this.poY = y;
			this.screenDist = screenDist;
		}
		
		protected void _drawLine( int x0, int y0, int x1, int y1, short r, short g, short b ) {
			if( x0 == x1 ) {
				if( y1 < y0 ) {
					int t = y1; y1 = y0; y0 = t;
				}
				if( y0 <  0 ) y0 = 0;
				if( y1 >= h ) y1 = h; 
				for( int y=y0, i=y0*w+x0; y<=y1; ++y, i += w ) {
					this.r[i] = r;
					this.g[i] = g;
					this.b[i] = b;
				}
			} else if( y0 == y1 ) {
				if( x1 < x0 ) {
					int t = x1; x1 = x0; x0 = t;
				}
				if( x0 <  0 ) x0 = 0;
				if( x1 >= w ) x1 = w; 
				for( int x=x0, i=y0*w+x0; x<=x1; ++x, ++i ) {
					this.r[i] = r;
					this.g[i] = g;
					this.b[i] = b;
				}
			} else if( Math.abs(x1 - x0) > Math.abs(y1 - y0) ) {
				int startx = Math.min(x0,x1);
				int endx   = Math.max(x0,x1);
				if( startx < 0 ) startx = 0;
				if( endx > w-1 ) endx = w-1;
				
				for( int x=startx; x<=endx; ++x ) {
					int y = y0 + (x-x0) * (y1+1-y0) / (x1+1-x0);
					if( y < 0 || y >= h ) continue;
					int i = y*w+x;
					this.r[i] = r;
					this.g[i] = g;
					this.b[i] = b;
				}
			} else {
				int starty = Math.min(y0,y1);
				int endy = Math.max(y0,y1);
				if( starty < 0 ) starty = 0;
				if( endy > h-1 ) endy = h-1;
				
				for( int y=starty; y<=endy; ++y ) {
					int x = x0 + (y-y0) * (x1+1-x0) / (y1+1-y0);
					if( x < 0 || x >= w ) continue;
					int i = y*w+x;
					this.r[i] = r;
					this.g[i] = g;
					this.b[i] = b;
				}
			}
		}
		
		public void drawLine( int x0, int y0, int x1, int y1, short r, short g, short b ) {
			_drawLine(x0, y0, x1, y1, r, g, b);
		}
		
		public void drawLine( float x0, float y0, float z0, float x1, float y1, float z1, short r, short g, short b) {
			project(x0, y0, z0);
			int px0 = (int)projectedX;
			int py0 = (int)projectedY;
			project(x1, y1, z1);
			int px1 = (int)projectedX;
			int py1 = (int)projectedY;
			drawLine( px0, py0, px1, py1, r, g, b );
		}
		
		public void drawAATrapezoid(int line0, int line1, int x0, int x1, int x2, int x3, short r, short g, short b) {
			for (int y = line0 < 0 ? 0 : line0; y < line1 && y < h; ++y) {
				int lx0 = x0 + (y - line0) * (x2 - x0) / (line1 - line0);
				int lx1 = x1 + (y - line0) * (x3 - x1) / (line1 - line0);
				if( lx0 <  0 ) lx0 = 0;
				if( lx1 >= w ) lx1 = w;
				for( int i = y * w + lx0, i1 = y * w + lx1; i < i1; ++i ) {
					this.r[i] = r;
					this.g[i] = g;
					this.b[i] = b;
				}
			}
		}
	}
	
	static class Eye {
		public float dx;
		public short r, g, b;
		public int div;
		
		public void setFilter( short r, short g, short b, int div ) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.div = div;
		}
	}
	
	static class BouncingSquare {
		public float x, y, z;
		public float rad = 10;
		public float dx, dy, dz;
		public short r = 128, g = 128, b = 128;
		
		public void updatePosition(float interval) {
			x += dx * interval;
			y += dy * interval;
			z += dz * interval;
		}
		
		public void bounce(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			if( x < minX && dx < 0 || x > maxX && dx > 0 ) dx = -dx;
			if( y < minY && dy < 0 || y > maxY && dy > 0 ) dy = -dy;
			if( z < minZ && dz < 0 || z > maxZ && dz > 0 ) dz = -dz;
		}
	}
	
	public static void main(String[] args) {
		final Frame f = new Frame("PolyDemo");
		final ImageCanvas c = new ImageCanvas();
		final int w = 320, h = 240;
		final int[] pixBuf = new int[w * h];
		final BufferedImage bImg = new BufferedImage(320, 240,
				BufferedImage.TYPE_INT_RGB);
		final Renderer renderer = new Renderer(w, h);
		final Compositor compositor = new Compositor(w, h);
		final Thread renderThread = new Thread() {
			final ArrayList<BouncingSquare> objects = new ArrayList<BouncingSquare>();
			final Eye[] eyes = new Eye[2];
			final int d = 100;
			
			@Override public void run() {
				eyes[0] = new Eye();
				eyes[0].dx = -20;
				eyes[0].setFilter((short)2,(short)0,(short)0, 1);
				eyes[1] = new Eye();
				eyes[1].dx = +20;
				eyes[1].setFilter((short)0,(short)2,(short)2, 1);
				
				for( int i=0; i<10; ++i ) {
					BouncingSquare object = new BouncingSquare();
					object.x = (float)Math.random()*w;
					object.y = (float)Math.random()*h;
					object.z = (float)Math.random()*d;
					object.dx = (float)Math.random();
					object.dy = (float)Math.random();
					object.dz = (float)Math.random();
					object.r = (short)(128+Math.random()*127);
					object.g = (short)(128+Math.random()*127);
					object.b = (short)(128+Math.random()*127);
					objects.add(object);
				}
				
				while( !Thread.interrupted() ) {
					compositor.fill((short)0, (short)0, (short)0);
					float speed = 20;
					int subframes = 4;
					for( int f=0; f<subframes; ++f ) {
						for( Eye e : eyes ) {
							renderer.fill((short)0, (short)0, (short)0);
							
							renderer.setPerspectiveOrigin(w/2 + e.dx, h/2, 100);
							renderer.drawLine(0  , 0  , 0, 0  , 0  , 100, (short)255, (short)255, (short)255);
							renderer.drawLine(w-1, 0  , 0, w-1, 0  , 100, (short)255, (short)255, (short)255);
							renderer.drawLine(0  , h-1, 0, 0  , h-1, 100, (short)255, (short)255, (short)255);
							renderer.drawLine(w-1, h-1, 0, w-1, h-1, 100, (short)255, (short)255, (short)255);
							renderer.drawLine(0  , 0  , 100, w-1, 0  , 100, (short)255, (short)255, (short)255);
							renderer.drawLine(0  , h-1, 100, w-1, h-1, 100, (short)255, (short)255, (short)255);
							renderer.drawLine(0  , 0  , 100, 0  , h-1, 100, (short)255, (short)255, (short)255);
							renderer.drawLine(w-1, 0  , 100, w-1, h-1, 100, (short)255, (short)255, (short)255);
							
							Collections.sort(objects, new Comparator<BouncingSquare>() {
								@Override public int compare(BouncingSquare o1, BouncingSquare o2) {
									if( o1.z > o2.z ) return -1;
									if( o1.z < o2.z ) return 1;
									return 0;
								}
							});
							for( BouncingSquare s : objects ) {
								renderer.project(s.x, s.y, s.z);
								final float drawX = renderer.projectedX;
								final float drawY = renderer.projectedY;
								final float scale = renderer.projectedScale;
								renderer.drawAATrapezoid(
									(int)(drawY-scale*s.rad), (int)(drawY+scale*s.rad),
									(int)(drawX-scale*s.rad), (int)(drawX+scale*s.rad),
									(int)(drawX-scale*s.rad), (int)(drawX+scale*s.rad),
									s.r, s.g, s.b);
								renderer.drawAATrapezoid(
									(int)(drawY-scale*s.rad)+2, (int)(drawY+scale*s.rad)-2,
									(int)(drawX-scale*s.rad)+2, (int)(drawX+scale*s.rad)-2,
									(int)(drawX-scale*s.rad)+2, (int)(drawX+scale*s.rad)-2,
									(short)0, (short)0, (short)0);
							}
							compositor.add(renderer, e.r, e.g, e.b, e.div);
						}
						
						for( BouncingSquare s : objects ) {
							s.updatePosition(speed/subframes);
							s.bounce(0, 0, 0, w, h, d);
						}
					}
					compositor.toArgb(pixBuf, (short)1, (short)1, (short)1, subframes);
					bImg.setRGB(0, 0, w, h, pixBuf, 0, w);
					c.setImage(bImg);
					try {
						Thread.sleep(10);
					} catch( InterruptedException e ) {
						return;
					}
				}
			}
		};
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				f.dispose();
				renderThread.interrupt();
			}
		});
		c.setSize(w * 2, h * 2);
		f.add(c);
		f.pack();
		f.setVisible(true);
		renderThread.start();
	}
}
