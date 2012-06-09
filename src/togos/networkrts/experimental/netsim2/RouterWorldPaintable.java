package togos.networkrts.experimental.netsim2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import togos.networkrts.awt.TimestampedPaintable;
import togos.networkrts.experimental.gensim.TimedEventQueue;
import togos.networkrts.experimental.gensim.Timestamped;
import togos.networkrts.inet.AddressUtil;

public class RouterWorldPaintable implements TimestampedPaintable, EventHandler
{
	protected RouterWorld world;
	protected double cx, cy, scale = 1.0;
	
	public TimedEventQueue newEventSink;
	public List<LiveEvent> activeEvents = new ArrayList<LiveEvent>();
	
	public RouterWorldPaintable() {}
	
	public RouterWorldPaintable( RouterWorld world ) {
		this.world = world;
	}
	
	@Override public synchronized void eventOccured(Timestamped event) throws Exception {
		if( event instanceof LiveEvent ) {
			activeEvents.add( (LiveEvent)event );
		}
	}
	
	protected synchronized void cleanActiveEvents( long timestamp ) {
		for( Iterator<LiveEvent> i=activeEvents.iterator(); i.hasNext(); ) {
			LiveEvent evt = i.next();
			if( !evt.isAlive(timestamp) ) {
				i.remove(); 
				continue;
			}
		}
	}
	
	protected void worldToScreenCoords( double wx, double wy, int screenWidth, int screenHeight, double[] dest ) {
		dest[0] = screenWidth  / 2 + (wx - cx) * scale;
		dest[1] = screenHeight / 2 + (wy - cy) * scale;
	}
	
	double clamp( double min, double max, double v ) {
		return v < min ? min : v > max ? max : v;
	}
	
	@Override
	public void paint( long timestamp, int width, int height, Graphics2D g2d ) {
		g2d.setFont( g2d.getFont().deriveFont( 10f ) );
		
		Rectangle clip = g2d.getClipBounds();
		g2d.setColor( Color.BLACK );
		g2d.fillRect( clip.x, clip.y, clip.width, clip.height );
		double[] c0 = new double[2];
		double[] c1 = new double[2];
		
		if( world == null ) return;
		
		g2d.setStroke( new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1, new float[]{2,2}, 0 ));
		g2d.setColor( Color.DARK_GRAY );
		for( RouterWorld.Router r : world.routers ) {
			for( RouterWorld.Router r1 : world.routers ) {
				if( r.id < r1.id ) continue;
				double dx = r1.x - r.x;
				double dy = r1.y - r.y;
				double dist = Math.sqrt(dx*dx+dy*dy);
				if( dist <= world.normalTransmissionIntensity ) {
					worldToScreenCoords(  r.x,  r.y, width, height, c0 );
					worldToScreenCoords( r1.x, r1.y, width, height, c1 );
					g2d.drawLine( (int)c0[0], (int)c0[1], (int)c1[0], (int)c1[1] );
				}
			}
		}
		
		Color macColor = new Color( 0, 0.8f, 0.8f, 0.5f );
		g2d.setColor( Color.GREEN );
		for( RouterWorld.Router r : world.routers ) {
			worldToScreenCoords( r.x, r.y, width, height, c0 );
			int sx = (int)c0[0];
			int sy = (int)c0[1];
			if( sx < clip.x || sy < clip.y || sx >= clip.x + clip.width || sy >= clip.y + clip.height ) continue;
			g2d.setColor( Color.GREEN );
			g2d.fillRect( sx, sy, (int)scale, (int)scale );
			g2d.setColor( macColor );
			g2d.drawString( AddressUtil.formatMacAddress(r.macAddress), sx, sy );
		}
		
		List<LiveEvent> events;
		synchronized( activeEvents ) {
			cleanActiveEvents(timestamp);
			events = new ArrayList(activeEvents);
		}
		
		for( LiveEvent evt : events ) {
			if( evt instanceof RouterWorld.WirelessTransmissionEvent ) {
				RouterWorld.WirelessTransmissionEvent wtEvt = (RouterWorld.WirelessTransmissionEvent)evt;
				worldToScreenCoords( wtEvt.sx, wtEvt.sy, width, height, c0 );
				double rad = wtEvt.getRadius(timestamp);
				double intensity = wtEvt.getIntensity(timestamp);
				if( intensity < 1 ) continue;
				
				g2d.setColor( new Color( 1, 1, 1, (float)clamp(0, 1, intensity / 10) ) );
				g2d.setStroke( new BasicStroke(1) );
				g2d.drawOval( (int)(c0[0]-rad*scale), (int)(c0[1]-rad*scale), (int)(rad*scale*2), (int)(rad*scale*2) );
			}
		}
	}
}
