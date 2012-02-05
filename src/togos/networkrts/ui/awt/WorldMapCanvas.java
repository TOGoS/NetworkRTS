package togos.networkrts.ui.awt;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import togos.networkrts.world.VisibleWorldState;
import togos.networkrts.world.WorldObject;

public class WorldMapCanvas extends Canvas
{
	private static final long serialVersionUID = 1L;
	
	VisibleWorldState visibleState;
	long cwx = 0, cwy = 0;
	double zoom = 1.0;
	
	public WorldMapCanvas() {
		final Collection objects = new ArrayList();
		
		WorldObject o = new WorldObject();
		o.x = 128;
		o.y = 128;
		o.z = 0;
		o.rad = 6;
		o.color = Color.GREEN;
		objects.add( o );
		
		o = new WorldObject();
		o.x = -128;
		o.y = -128;
		o.z = 0;
		o.rad = 6;
		o.color = Color.WHITE;
		objects.add( o );
		
		visibleState = new VisibleWorldState() {
			public Collection getVisibleWorldObjects() {
				return objects;
			}
			
			public Collection getVisibleAreas() {
				return Collections.EMPTY_LIST;
			}
		};
	}
	
	public void paint( WorldObject o, double x, double y, double scale, Graphics g ) {
		g.setColor( o.color );
		g.fillRect( (int)x, (int)y, (int)(o.rad*2*scale), (int)(o.rad*2*scale) );
	}
	
	public void paint( double cwx, double cwy, double scale, Graphics g ) {
		int csx = getWidth()/2;
		int csy = getHeight()/2;
		for( Iterator i=visibleState.getVisibleWorldObjects().iterator(); i.hasNext(); ) {
			WorldObject o = (WorldObject)i.next();
			paint( o, (o.x - cwx)*scale + csx, (o.y - cwy)*scale + csy, scale, g );
		}
	}
	
	public void paint( Graphics g ) {
		paint( cwx, cwx, zoom, g );
	}
	
	
	public static void main( String[] args ) {
		Frame f = new Frame("NetworkRTS");
		
		WorldMapCanvas c = new WorldMapCanvas();
		c.setPreferredSize(new Dimension(640,480));
		c.setBackground(Color.BLACK);
		
		f.add(c);
		f.pack();
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				e.getWindow().dispose();
			}
		});
		f.setVisible(true);
	}
}
