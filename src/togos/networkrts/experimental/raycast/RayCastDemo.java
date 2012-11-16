package togos.networkrts.experimental.raycast;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import togos.networkrts.tfunc.ColorFunction;
import togos.networkrts.tfunc.ConstantColorFunction;

public class RayCastDemo
{
	interface Clock {
		public long getTime();
	}
	
	public static final class SystemClock implements Clock {
		static final SystemClock instance = new SystemClock();
		
		private SystemClock() { }
		
		@Override
		public long getTime() { return System.currentTimeMillis(); }
	}
	
	interface ZoomDrawable {
		public void draw( Graphics g, double cx, double cy, double zoom, long time );
	}
	
	static class CenterMarker implements ZoomDrawable {
		ColorFunction pointColor = new ConstantColorFunction(0xFFFF0000);
		
		@Override
		public void draw( Graphics g, double cx, double cy, double zoom, long time ) {
			g.setColor(pointColor.getAwtColor(time));
			g.drawLine( (int)(cx-0.25*zoom), (int)(cy-0.25*zoom), (int)(cx+0.25*zoom), (int)(cy+0.25*zoom) );
			g.drawLine( (int)(cx+0.25*zoom), (int)(cy-0.25*zoom), (int)(cx-0.25*zoom), (int)(cy+0.25*zoom) );
		}
	}
	
	static class RadarDisplay implements ZoomDrawable {
		ColorFunction pointColor = new ConstantColorFunction(0xFFFFFFFF);
		double[] points = new double[] { 1, 2, 3, 2, 1, 2, 3, 2, 1 } ;
		int pointSize = 1;
		
		@Override
		public void draw( Graphics g, double cx, double cy, double zoom, long time ) {
			g.setColor( pointColor.getAwtColor(time) );
			for( int i=0; i<points.length; ++i ) {
				double dist = Math.pow( points[i], 0.7 );
				double angle = 2 * Math.PI * i / points.length;
				double x = cx + dist * zoom * Math.cos(angle);
				double y = cy + dist * zoom * Math.sin(angle);
				int px = (int)Math.floor(x);
				int py = (int)Math.floor(y);
				g.fillRect( px, py, pointSize, pointSize );
			}
		}
	}
	
	static class ZoomDrawCanvas extends Canvas {
		private static final long serialVersionUID = 1517818602701962997L;
		
		public List<ZoomDrawable> zds = new ArrayList();
		public double zoom = 10;
		public Clock clock = SystemClock.instance;
		
		@Override
		public void paint( Graphics g ) {
			for( ZoomDrawable zd : zds ) {
				zd.draw( g, getWidth()/2, getHeight()/2, zoom, clock.getTime() );
			}
		}
	}
	
	static class RayCaster {
		int w, h;
		boolean[] walls;
		
		public RayCaster( int w, int h ) {
			this.w = w;
			this.h = h;
			this.walls = new boolean[w*h];
		}
		
		protected static int tmod( int num, int den ) {
			int rem = num % den;
			return rem < 0 ? den + rem : rem;
		}
		
		protected void putWallAt( int x, int y, boolean v ) {
			walls[w*tmod(y,h)+tmod(x,w)] = v;
		}
		
		protected boolean wallAt( int x, int y ) {
			return walls[w*tmod(y,h)+tmod(x,w)];
		}
		
		protected boolean wallAt( double x, double y ) {
			return wallAt( (int)Math.floor(x), (int)Math.floor(y) );
		}
		
		public void cast( double x, double y, double angle0, double dAngle, double[] dest ) {
			if( wallAt( (int)x, (int)y ) ) {
				for( int i=0; i<dest.length; ++i ) dest[i] = 0;
				return;
			}
			
			double angle = angle0;
			for( int i=0; i<dest.length; ++i, angle += dAngle ) {
				double dist = 0;
				
				// 'current position' of trace
				double cx = x, cy = y;
				
				// Unit x, y of cast ray
				final double ux = Math.cos(angle);
				final double uy = Math.sin(angle);
				final double uSteepness = Math.abs(uy/ux);
				
				while( dist < 100 ) {
					// Distance to cell borders
					double cdx = ux > 0 ? Math.ceil(cx)-cx : Math.floor(cx)-cx;
					double cdy = uy > 0 ? Math.ceil(cy)-cy : Math.floor(cy)-cy;
					
					// If cast ray is 'steeper' than that to corner of cell...
					if( cdx == 0 ) cdx = ux > 0 ? 1 : -1;
					if( cdy == 0 ) cdy = uy > 0 ? 1 : -1;
					
					if( uSteepness > Math.abs(cdy/cdx) ) {
						// Then we move up/down
						cy = Math.round(cy + cdy);
						cx += ux*cdy/uy;
						dist += Math.abs(cdy/uy);
						if( wallAt(cx, cy) || wallAt(cx, cy-1) ) break;
					} else {
						// Otherwise we move to the left or right
						cx = Math.round(cx + cdx);
						cy += uy*cdx/ux;
						dist += Math.abs(cdx/ux);
						if( wallAt(cx, cy) || wallAt(cx-1, cy) ) break;
					}
				}
				
				dest[i] = dist >= 100 ? Double.POSITIVE_INFINITY : dist;
			}
		}
	}
	
	static class RobotState {
		double px = 16.5, py = 16.5;
		double[] distanceReadings = new double[360];
	}
	
	public static void main( String[] args ) {
		final RobotState rs = new RobotState();
		final RayCaster rc = new RayCaster( 32, 32 );
		for( int i=0; i<1024; ++i ) rc.walls[i] = true;
		
		Random r = new Random();
		int tx = 16, ty = 16;
		for( int i=0; i<1024; ++i ) {
			rc.putWallAt( tx, ty, false );
			//ty += 1;
			
			if( r.nextBoolean() ) {
				tx += (r.nextBoolean() ? 1 : -1);
			} else {
				ty += (r.nextBoolean() ? 1 : -1);
			}
			
		}
		
		rc.cast( rs.px, rs.py, 0, Math.PI*2/360, rs.distanceReadings );
		
		/*
		for( int i=0; i<dist.length; ++i ) {
			dist[i] = Math.sqrt(dist[i]); //Math.sqrt(dist[i]);
		}
		*/
		
		
		final Frame window = new Frame("Robot Client");
		final ZoomDrawCanvas zdCanvas = new ZoomDrawCanvas();
		zdCanvas.setPreferredSize( new Dimension(512,384) );
		zdCanvas.setBackground( Color.BLACK );
		RadarDisplay rd = new RadarDisplay();
		rd.points = rs.distanceReadings;
		zdCanvas.zds.add(rd);
		zdCanvas.zds.add(new CenterMarker());
		zdCanvas.zoom = 64;
		zdCanvas.addKeyListener( new KeyListener() {
			@Override public void keyPressed( KeyEvent kevt ) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): rs.py -= 1; break;
				case( KeyEvent.VK_DOWN  ): rs.py += 1; break;
				case( KeyEvent.VK_LEFT  ): rs.px -= 1; break;
				case( KeyEvent.VK_RIGHT ): rs.px += 1; break;
				default:
					System.err.println(kevt.getKeyCode());
				}
				rc.cast( rs.px, rs.py, 0, Math.PI*2/360, rs.distanceReadings );
				zdCanvas.repaint();
			}
			@Override public void keyReleased( KeyEvent kevt ) {
			}
			@Override public void keyTyped( KeyEvent kevt ) {
			}
		});
		window.add(zdCanvas);
		window.pack();
		window.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				window.dispose();
			}
		});
		window.setVisible(true);
		zdCanvas.requestFocus();
	}
}
