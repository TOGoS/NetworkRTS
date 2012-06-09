package togos.networkrts.experimental.netsim2;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import togos.blob.util.BlobUtil;
import togos.networkrts.experimental.gensim.Timestamped;

public class RouterWorld implements EventHandler
{
	class Router {
		public final long id;
		public double x, y;
		public final byte[] macAddress;
		public final Set<Router> wireLinks = new HashSet<Router>();
		public byte[] ip6Address = new byte[16];
		public int prefixLength = 128;
		
		public Router( long id, byte[] macAddress ) {
			this.id = id;
			this.macAddress = macAddress;
		}
	}
	
	public Sink<Timestamped> eventScheduler;
	public final Set<Router> routers = new HashSet<Router>();
	double normalTransmissionIntensity = 100; 
	double c = 20;
	
	Random rand = new Random();
	
	protected synchronized byte[] newMacAddress() {
		byte[] mac = new byte[6];
		rand.nextBytes(mac);
		return mac;
	}
	
	long nextRouterId = 1;
	
	public void initRouters( int count ) {
		for( ; count > 0; --count ) {
			Router r = new Router( nextRouterId++, newMacAddress() );
			r.x = rand.nextInt(1024);
			r.y = rand.nextInt(1024);
			routers.add(r);
		}
	}
	
	static class Frame {
		public final byte[] sourceMacAddress;
		public final byte[] destMacAddress;
		public final Object payload;
		
		public Frame( byte[] sm, byte[] dm, Object p ) {
			this.sourceMacAddress = sm;
			this.destMacAddress = dm;
			this.payload = p;
		}
	}
	
	static class AddressAnnouncementPacket {
		public final byte[] address;
		/** Length of parent network addresses */
		public final int prefixLength;
		
		public AddressAnnouncementPacket( byte[] address, int prefixLength ) {
			this.address = address;
			this.prefixLength = prefixLength;
		}
	}
	
	static class WirelessTransmissionEvent implements LiveEvent {
		public final double sx, sy;
		public final long time;
		public final double speed;
		public final double intensity; // at radius = 1
		public final Frame data;
		
		public WirelessTransmissionEvent( double sx, double sy, long timestamp, double speed, double intensity, Frame data ) {
			this.sx = sx;
			this.sy = sy;
			this.time = timestamp;
			this.speed = speed;
			this.intensity = intensity;
			this.data = data;
		}
		
		@Override public long getTimestamp() {
			return time;
		}
		public double getRadius( long t ) {
			return (t - time) * speed / 1000;
		}
		public double getIntensity( long t ) {
			return intensity / getRadius(t);
		}
		public boolean isAlive( long t ) {
			if( t <= time ) return true;
			return getIntensity(t) >= 1;
		}
	}
	
	static class FrameReceptionEvent implements Timestamped {
		public final Router destination;
		public final long timestamp;
		public final Frame data;
		
		public FrameReceptionEvent( Router dest, long timestamp, Frame data ) {
			this.destination = dest;
			this.timestamp = timestamp;
			this.data = data;
		}
		
		@Override public long getTimestamp() {
			return timestamp;
		}
	}
	
	public static final byte[] BROADCAST_MAC_ADDRESS = new byte[]{-1,-1,-1,-1,-1,-1};
	
	@Override public void eventOccured( Timestamped evt ) throws Exception {
		if( evt instanceof WirelessTransmissionEvent ) {
			WirelessTransmissionEvent wtEvt = (WirelessTransmissionEvent)evt;
			if( wtEvt.data instanceof Frame ) {
				for( Router r : routers ) {
					double dx = r.x - wtEvt.sx;
					double dy = r.y - wtEvt.sy;
					double dist = Math.sqrt(dx*dx+dy*dy);
					double interval = dist / wtEvt.speed;
					long receptionTime = wtEvt.getTimestamp() + (long)(interval*1000);
					double receptionIntensity = wtEvt.getIntensity( receptionTime );
					if( dist > 0 && receptionIntensity >= 1 ) {
						eventScheduler.give( new FrameReceptionEvent(r, receptionTime, wtEvt.data) );
					}
				}
			}
		} else if( evt instanceof FrameReceptionEvent ) {
			FrameReceptionEvent frEvt = (FrameReceptionEvent)evt;
			Router dest = frEvt.destination;
			Object payload = frEvt.data.payload;
			if( !BlobUtil.equals(BROADCAST_MAC_ADDRESS,frEvt.data.destMacAddress) && !BlobUtil.equals(frEvt.destination.macAddress, frEvt.data.destMacAddress) ) {
				System.err.println("Drop");
				return;
			}
			
			if( payload instanceof AddressAnnouncementPacket ) {
				AddressAnnouncementPacket aap = (AddressAnnouncementPacket)payload;
				if( dest.prefixLength > aap.prefixLength+8 ) {
					byte[] newAddy = new byte[16];
					int i;
					for( i=0; i<aap.prefixLength/8; ++i ) {
						newAddy[i] = aap.address[i]; 
					}
					newAddy[i++] = (byte)(1+rand.nextInt(253));
					for( ; i<16; ++i ) newAddy[i] = 1;
					dest.ip6Address = newAddy;
					dest.prefixLength = aap.prefixLength+8;
					eventScheduler.give( new WirelessTransmissionEvent(frEvt.destination.x, frEvt.destination.y, frEvt.timestamp, c, normalTransmissionIntensity,
						new RouterWorld.Frame( dest.macAddress, new byte[]{-1,-1,-1,-1,-1,-1},
							new RouterWorld.AddressAnnouncementPacket( dest.ip6Address, dest.prefixLength )
						)
					));
				}
			}
		}
	}
}
