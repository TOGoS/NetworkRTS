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
				short sg = (short) (this.g[i] * g / div);
				short sb = (short) (this.b[i] * b / div);
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
		protected short drawR, drawG, drawB;
		
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
		
		public void setColor( short r, short g, short b ) {
			this.drawR = r;
			this.drawG = g;
			this.drawB = b;
		}
		
		protected void _drawLine( int x0, int y0, int x1, int y1 ) {
			if( x0 == x1 ) {
				if( y1 < y0 ) {
					int t = y1; y1 = y0; y0 = t;
				}
				if( y0 <  0 ) y0 = 0;
				if( y1 >= h ) y1 = h; 
				for( int y=y0, i=y0*w+x0; y<=y1; ++y, i += w ) {
					this.r[i] = drawR;
					this.g[i] = drawG;
					this.b[i] = drawB;
				}
			} else if( y0 == y1 ) {
				if( x1 < x0 ) {
					int t = x1; x1 = x0; x0 = t;
				}
				if( x0 <  0 ) x0 = 0;
				if( x1 >= w ) x1 = w; 
				for( int x=x0, i=y0*w+x0; x<=x1; ++x, ++i ) {
					this.r[i] = drawR;
					this.g[i] = drawG;
					this.b[i] = drawB;
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
					this.r[i] = drawR;
					this.g[i] = drawG;
					this.b[i] = drawB;
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
					this.r[i] = drawR;
					this.g[i] = drawG;
					this.b[i] = drawB;
				}
			}
		}
		
		public void drawLine( int x0, int y0, int x1, int y1 ) {
			_drawLine(x0, y0, x1, y1);
		}
		
		public void drawLine( float x0, float y0, float z0, float x1, float y1, float z1) {
			project(x0, y0, z0);
			int px0 = (int)projectedX;
			int py0 = (int)projectedY;
			project(x1, y1, z1);
			int px1 = (int)projectedX;
			int py1 = (int)projectedY;
			drawLine( px0, py0, px1, py1 );
		}
		
		public void drawAATrapezoid(float line0, float line1, float x0, float x1, float x2, float x3 ) {
			for (int y = line0 < 0 ? 0 : (int)Math.round(line0); y < (int)Math.round(line1) && y < h; ++y) {
				int lx0 = (int)(x0 + (y + 0.5f - line0) * (x2 - x0) / (line1 - line0));
				int lx1 = (int)(x1 + (y + 0.5f - line0) * (x3 - x1) / (line1 - line0));
				if( lx0 <  0 ) lx0 = 0;
				if( lx1 >= w ) lx1 = w;
				for( int i = y * w + lx0, i1 = y * w + lx1; i < i1; ++i ) {
					this.r[i] = drawR;
					this.g[i] = drawG;
					this.b[i] = drawB;
				}
			}
		}
		
		public void drawTriangle(float x0, float y0, float x1, float y1, float x2, float y2) {
			// Sort verteces
			
			float t;
			
			if( y0 <= y1 && y0 <= y2 ) {
			} else if( y1 <= y0 && y1 <= y2 ) {
				t = y1; y1 = y0; y0 = t;
				t = x1; x1 = x0; x0 = t;
			} else {
				t = y2; y2 = y0; y0 = t;
				t = x2; x2 = x0; x0 = t;
			}
			
			if( y1 > y2 ) {
				t = y2; y2 = y1; y1 = t;
				t = x2; x2 = x1; x1 = t;
			}
			
			if( y2 == y0 ) return;
			
			float x4 = x0+(x2-x0)*(y1-y0)/(y2-y0);
			if( x1 > x4 ) {
				t = x1; x1 = x4; x4 = t;
			}
			drawAATrapezoid(y0, y1, x0, x0, x1, x4);
			drawAATrapezoid(y1, y2, x1, x4, x2, x2);
		}
		
		public void drawTriangle(float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2) {
			project(x0, y0, z0); int px0 = (int)projectedX, py0 = (int)projectedY;
			project(x1, y1, z1); int px1 = (int)projectedX, py1 = (int)projectedY;
			project(x2, y2, z2); int px2 = (int)projectedX, py2 = (int)projectedY;
			drawTriangle(px0, py0, px1, py1, px2, py2);
		}
		
		public void drawQuad(
			float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3
		) {
			drawTriangle( x0, y0, z0, x1, y1, z1, x2, y2, z2 );
			drawTriangle( x2, y2, z2, x3, y3, z3, x0, y0, z0 );
		}
		
		protected void shade( short r, short g, short b, int shade ) {
			setColor((short)(r*shade/255), (short)(g*shade/255), (short)(b*shade/255));
		}
		
		public void drawCuboid( float x0, float y0, float z0, float x1, float y1, float z1 ) {
			short r = drawR, g = drawG, b = drawB;
			
			// Back
			shade(r,g,b, 128);
			drawQuad(
				x1, y0, z1,
				x0, y0, z1,
				x0, y1, z1,
				x1, y1, z1
			);
			// Left
			shade(r,g,b, 192);
			drawQuad(
				x0, y0, z1,
				x0, y0, z0,
				x0, y1, z0,
				x0, y1, z1
			);
			// Right
			shade(r,g,b, 160);
			drawQuad(
				x1, y0, z0,
				x1, y0, z1,
				x1, y1, z1,
				x1, y1, z0
			);
			// Top
			shade(r,g,b, 255);
			drawQuad(
				x0, y0, z1,
				x1, y0, z1,
				x1, y0, z0,
				x0, y0, z0
			);
			// Bottom
			shade(r,g,b, 96);
			drawQuad(
				x0, y1, z0,
				x1, y1, z0,
				x1, y1, z1,
				x0, y1, z1
			);
			// Front
			shade(r,g,b, 224);
			drawQuad(
				x0, y0, z0,
				x1, y0, z0,
				x1, y1, z0,
				x0, y1, z0
			);
		}
		
		public void drawCube( float x, float y, float z, float rad ) {
			drawCuboid( x-rad, y-rad, z-rad, x+rad, y+rad, z+rad );
		}
	}
	
	static class Eye {
		public float dx;
		public short r=1, g=1, b=1;
		public int div=1;
		
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
			final int d = w, dist = w * 2;
			
			@Override public void run() {
				
				eyes[0] = new Eye();
				eyes[0].dx = -20;
				eyes[0].setFilter((short)1,(short)0,(short)0, 1);
				eyes[1] = new Eye();
				eyes[1].dx = +20;
				eyes[1].setFilter((short)0,(short)1,(short)1, 1);
				
				//eyes[0] = new Eye();
				
				for( int i=0; i<5; ++i ) {
					BouncingSquare object = new BouncingSquare();
					object.x = (float)Math.random()*w;
					object.y = (float)Math.random()*h;
					object.z = (float)Math.random()*d;
					object.rad = 30;
					object.dx = (float)Math.random();
					object.dy = (float)Math.random();
					object.dz = (float)Math.random();
					object.r = (short)(128+Math.random()*127);
					object.g = (short)(128+Math.random()*127);
					object.b = (short)(128+Math.random()*127);
					objects.add(object);
				}
				for( int i=0; i<70; ++i ) {
					BouncingSquare object = new BouncingSquare();
					object.x = (float)Math.random()*w;
					object.y = (float)h;
					object.z = (float)Math.random()*d;
					object.rad = 2;
					object.r = (short)(128+Math.random()*127);
					object.g = (short)(128);
					object.b = (short)(128);
					object.dx = (float)Math.random() * 0.1f;
					object.dy = (float)Math.random() * 0.1f;
					object.dz = (float)Math.random() * 0.1f;
					objects.add(object);
				}
				
				
				while( !Thread.interrupted() ) {
					compositor.fill((short)0, (short)0, (short)0);
					float speed = 15;
					int subframes = 4;
					for( int f=0; f<subframes; ++f ) {
						for( Eye e : eyes ) {
							renderer.fill((short)32, (short)48, (short)32);
							
							// Draw walls
							renderer.setPerspectiveOrigin(w/2 + e.dx, h/2, dist);
							
							renderer.setColor((short)128, (short)128, (short)128);
							renderer.drawQuad(0  , 0  , 0  ,
											  0  , 0  , d  ,
											  0  , h  , d  ,
											  0  , h  , 0  );
							renderer.setColor((short)192, (short)192, (short)192);
							renderer.drawQuad(w  , 0  , d  ,
											  w  , 0  , 0  ,
											  w  , h  , 0  ,
											  w  , h  , d  );
							renderer.setColor((short) 96, (short) 96, (short) 96);
							renderer.drawQuad(0  , 0  , 0  ,
											  w  , 0  , 0  ,
											  w  , 0  , d  ,
											  0  , 0  , d  );
							renderer.setColor((short)224, (short)224, (short)224);
							renderer.drawQuad(0  , h  , d  ,
											  w  , h  , d  ,
											  w  , h  , 0  ,
											  0  , h  , 0  );
							renderer.setColor((short)160, (short)160, (short)160);
							renderer.drawQuad(0  , 0  , d  ,
											  w  , 0  , d  ,
											  w  , h  , d  ,
											  0  , h  , d  );
							
							// Draw floor dots
							
							Collections.sort(objects, new Comparator<BouncingSquare>() {
								@Override public int compare(BouncingSquare o1, BouncingSquare o2) {
									if( o1.z > o2.z ) return -1;
									if( o1.z < o2.z ) return 1;
									return 0;
								}
							});
							for( BouncingSquare s : objects ) {
								renderer.project(s.x, s.y, s.z);
								renderer.setColor(s.r, s.g, s.b);
								renderer.drawCube(s.x, s.y, s.z, s.rad);

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
