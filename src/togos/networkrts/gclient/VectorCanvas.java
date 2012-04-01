package togos.networkrts.gclient;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;

import togos.networkrts.simulation.Simulator;
import togos.networkrts.world.PositionFunction;
import togos.networkrts.world.WorldObject;

public class VectorCanvas extends Canvas
{
	private static final long serialVersionUID = 1L;
	
	long cwx = 0, cwy = 0;
	double zoom = 1.0, rotation = 0.0;
	
	Simulator sim = new Simulator();
	
	public VectorCanvas() {
	}
	
	protected long curTime() {
		return System.currentTimeMillis();
	}
	
	public void paint( WorldObject o, long ts, Graphics g, double x, double y, double scale ) {
		g.setColor( o.color );
		g.fillRect( (int)x, (int)y, (int)(o.rad*2*scale), (int)(o.rad*2*scale) );
	}
	
	public void paint( double cwx, double cwy, Graphics g, double scale ) {
		int csx = getWidth()/2;
		int csy = getHeight()/2;
		long ts = curTime();
		for( Iterator i=sim.getAllObjects().iterator(); i.hasNext(); ) {
			WorldObject o = (WorldObject)i.next();
			PositionFunction pf = o.pos;
			paint( o, curTime(), g, (pf.getX(ts) - cwx)*scale + csx, (pf.getY(ts) - cwy)*scale + csy, scale );
		}
	}
	
	public void paint( Graphics g ) {
		paint( cwx, cwx, g, zoom );
	}
	
	
	public static void main( String[] args ) {
		Frame f = new Frame("NetworkRTS");
		
		final VectorCanvas c = new VectorCanvas();
		c.setPreferredSize(new Dimension(640,480));
		c.setBackground(Color.BLACK);
		
		final Thread t = new Thread() {
			public void run() {
				long t = System.currentTimeMillis(); 
				try {
					while( !Thread.interrupted() ) {
						long destTime = t+100;
						c.sim.runUntil(destTime);
						c.repaint();
						long sleepTime = destTime - System.currentTimeMillis();
						if( sleepTime > 0 ) Thread.sleep( sleepTime );
						t = destTime;
					}
				} catch( InterruptedException e ) {
				}
			};
		};
		t.start();
		
		f.add(c);
		f.pack();
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				e.getWindow().dispose();
				t.interrupt();
			}
		});
		f.setVisible(true);
	}
}
