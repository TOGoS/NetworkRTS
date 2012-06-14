package togos.networkrts.experimental.netsim2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import togos.networkrts.awt.TimestampedPaintable;
import togos.networkrts.experimental.gensim.TimedEventQueue;
import togos.networkrts.experimental.gensim.Timestamped;
import togos.networkrts.experimental.netsim2.RouterWorld.Router;
import togos.networkrts.experimental.netsim2.RouterWorld.TransmitterType;
import togos.networkrts.experimental.shape.TCircle;
import togos.networkrts.experimental.shape.TRectangle;
import togos.networkrts.inet.AddressUtil;

public class RouterWorldPaintable implements TimestampedPaintable, EventHandler
{
	protected RouterWorld world;
	protected double cx, cy, scale = 1.0;
	
	public TimedEventQueue newEventSink;
	public List<LiveEvent> activeEvents = new ArrayList<LiveEvent>();
	
	public Font statsFont = new Font("Monospaced", Font.PLAIN, 12);
	public String statusText;
	
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
	
	protected void screenToWorldCoords( double sx, double sy, int screenWidth, int screenHeight, double[] dest ) {
		dest[0] = cx + (sx - (screenWidth  / 2)) / scale;
		dest[1] = cy + (sy - (screenHeight / 2)) / scale;
	}
	
	protected void worldToScreenCoords( double wx, double wy, int screenWidth, int screenHeight, double[] dest ) {
		dest[0] = screenWidth  / 2 + (wx - cx) * scale;
		dest[1] = screenHeight / 2 + (wy - cy) * scale;
	}
	
	double clamp( double min, double max, double v ) {
		return v < min ? min : v > max ? max : v;
	}
	
	static class RouterPair {
		protected final Router r1, r2;
		public RouterPair( Router r1, Router r2 ) {
			this.r1 = r1; this.r2 = r2;
		}
		@Override public int hashCode() {
			return r1.hashCode() + r2.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if( !(obj instanceof RouterPair) ) return false;
			
			RouterPair rp1 = (RouterPair)obj;
			return (rp1.r1 == r1 && rp1.r2 == r2) || (rp1.r1 == r2 && rp1.r2 == r1); 
		}
	}
	
	@Override
	public void paint( final long timestamp, final int width, final int height, final Graphics2D g2d ) {
		Font originalFont = g2d.getFont();
		
		final Rectangle clip = g2d.getClipBounds();
		g2d.setColor( Color.BLACK );
		g2d.fillRect( clip.x, clip.y, clip.width, clip.height );
		final double[] c0 = new double[2];
		final double[] c1 = new double[2];
		
		if( world == null ) return;
		
		g2d.setFont( originalFont.deriveFont( 10f ) );
		
		g2d.setStroke( new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1, new float[]{2,2}, 0 ));
		g2d.setColor( Color.DARK_GRAY );
		
		screenToWorldCoords( clip.getMinX(), clip.getMinY(), width, height, c0 );
		screenToWorldCoords( clip.getMaxX(), clip.getMaxY(), width, height, c1 );
		TRectangle worldScreenClip = new TRectangle(c0[0], c0[1], c1[0]-c0[0], c1[1]-c0[1]);
		
		final int[] drawCounters = new int[3];
		
		try {
			world.routerEntree.forEachObject(0, Long.MAX_VALUE, worldScreenClip, new Sink<Router>() {
				final HashSet<RouterPair> linksDrawn = new HashSet<RouterPair>();
				
				public void give(final Router r0) throws Exception {
					for( final TransmitterType tt0 : r0.getTransmitters() ) {
						world.routerEntree.forEachObject(tt0.channelFlag, Long.MAX_VALUE, new TCircle(r0.x, r0.y, tt0.power), new Sink<Router>() {
							public void give(final Router r1) throws Exception {
								if( r0 == r1 ) return;
								
								++drawCounters[2];
								
								RouterPair rp = new RouterPair(r0,r1);
								if( linksDrawn.contains(rp) ) return;
								linksDrawn.add(rp);
								
								double dx = r1.x - r0.x;
								double dy = r1.y - r0.y;
								double dist = Math.sqrt(dx*dx+dy*dy)-Math.min(r0.getMaxRadius(),r1.getMaxRadius());
								
								for( TransmitterType tt1 : r1.getTransmitters() ) {
									if( tt0.channelFlag == tt1.channelFlag ) {
										double maxDist = Math.min(tt0.power,tt1.power);
										if( dist <= maxDist ) {
											worldToScreenCoords(  r0.x,  r0.y, width, height, c0 );
											worldToScreenCoords( r1.x, r1.y, width, height, c1 );
											g2d.setColor(tt0.color);
											g2d.drawLine( (int)c0[0], (int)c0[1], (int)c1[0], (int)c1[1] );
											++drawCounters[1];
										}
									}
								}
							}
						});
					}
				}
			});
			
			final Color macColor = new Color( 0, 0.8f, 0.8f, 0.5f );
			final Color ip6Color = new Color( 0.6f, 0.8f, 0.8f, 0.5f );
			g2d.setColor( Color.GREEN );
			world.routerEntree.forEachObject(0, Long.MAX_VALUE, worldScreenClip, new Sink<Router>() {
				public void give(Router r) throws Exception {
					int size = (int)r.getMaxRadius();
					worldToScreenCoords( r.x, r.y, width, height, c0 );
					int sx = (int)c0[0];
					int sy = (int)c0[1];
					// if( sx < clip.x || sy < clip.y || sx >= clip.x + clip.width || sy >= clip.y + clip.height ) return;
					int w = (int)scale*size;
					// if( w == 0 ) w = 1;
					g2d.setColor( Color.GREEN );
					g2d.fillRect( sx-(int)(scale*size/2), sy-(int)(scale*size/2), w, w );
					g2d.setColor( macColor );
					g2d.drawString( AddressUtil.formatMacAddress(r.macAddress), sx, sy );
					if( r.ip6Address[0] != 0 ) {
						g2d.setColor( ip6Color );
						g2d.drawString( AddressUtil.formatIp6Address(r.ip6Address,0)+"/"+r.ip6PrefixLength, sx, sy+12 );
					}
					++drawCounters[0];
				}
			});
		} catch( Exception e ) {
			e.printStackTrace();
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
				
				Color base = Color.WHITE;
				if( wtEvt.data.payload instanceof RouterWorld.AddressGivementPacket ) {
					base = Color.YELLOW;
				} else if( wtEvt.data.payload instanceof RouterWorld.AddressRequestPacket ) {
					base = Color.ORANGE;
				} else if( wtEvt.data.payload instanceof RouterWorld.AddressAnnouncementPacket ) {
					base = Color.GREEN;
				}
				
				g2d.setColor( new Color( base.getRed()/255f, base.getGreen()/255f, base.getBlue()/255f, (float)clamp(0, 1, intensity / 10) ) );
				g2d.setStroke( new BasicStroke(1) );
				g2d.drawOval( (int)(c0[0]-rad*scale), (int)(c0[1]-rad*scale), (int)(rad*scale*2), (int)(rad*scale*2) );
			}
		}
		
		if( this.statsFont != null ) g2d.setFont( statsFont );
		g2d.setColor( Color.GREEN );
		
		g2d.drawString( "Hit F1 for help", 12, 24 );
		g2d.drawString( "Pings sent:     "+world.pingsSent, 12, 24+18 );
		g2d.drawString( "Pongs sent:     "+world.pongsSent, 12, 24+18*2 );
		g2d.drawString( "Pongs received: "+world.pongsReceived, 12, 24+18*3);
		
		g2d.drawString( String.format("Drew %04d lines, %04d routers, %04d inner loops", drawCounters[1], drawCounters[0], drawCounters[2]), 12, height-6-18 );
		
		if( statusText != null ) {
			g2d.drawString( statusText, 12, height-6 );
		}
	}
}
