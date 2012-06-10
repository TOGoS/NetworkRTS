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
		public byte[][] addressesAllocated = new byte[16][];
		public int prefixLength = 128;
		
		public Router( long id, byte[] macAddress ) {
			this.id = id;
			this.macAddress = macAddress;
		}
	}
	
	public Sink<Timestamped> eventScheduler;
	public final Set<Router> routers = new HashSet<Router>();
	double normalTransmissionIntensity = 100; 
	double c = 100;
	
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
		public final int childBits;
		
		public AddressAnnouncementPacket( byte[] address, int prefixLength, int childBits ) {
			this.address = address;
			this.prefixLength = prefixLength;
			this.childBits = childBits;
		}
	}
	
	static class AddressRequestPacket {}
	
	static class AddressGivementPacket {
		public final byte[] address;
		public final int prefixLength;
		
		public AddressGivementPacket( byte[] address, int prefixLength ) {
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
	
	protected void sendWireless( long ts, Router source, byte[] destMac, Object payload ) throws Exception {
		eventScheduler.give( new WirelessTransmissionEvent( source.x, source.y, ts, c, normalTransmissionIntensity,
			new RouterWorld.Frame( source.macAddress, destMac, payload )
		));
	}
	
	static byte setHigh( byte o, int n ) {
		return (byte)((o & 0x0F) | ((n<<4)& 0xF0));
	}
	
	static byte setLow( byte o, int n ) {
		return (byte)((o & 0xF0) | (n & 0x0F));
	}
	
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
			Frame frame = frEvt.data;
			Router dest = frEvt.destination;
			Object payload = frame.payload;
			if( !BlobUtil.equals(BROADCAST_MAC_ADDRESS,frame.destMacAddress) && !BlobUtil.equals(frEvt.destination.macAddress, frame.destMacAddress) ) {
				return;
			}
			
			if( payload instanceof AddressAnnouncementPacket ) {
				AddressAnnouncementPacket aap = (AddressAnnouncementPacket)payload;
				if( dest.ip6Address[0] == 0 || dest.prefixLength > aap.prefixLength+aap.childBits ) {
					// Request an address from the sender
					sendWireless( frEvt.getTimestamp(), dest, frame.sourceMacAddress, new RouterWorld.AddressRequestPacket() );
				}
			} else if( payload instanceof AddressRequestPacket ) {
				int subnetBits = 4;
				if( dest.prefixLength + subnetBits > 128 ) return;
				int minNum, endNum=15;
				if( dest.prefixLength + subnetBits == 128 ) {
					minNum = 1;
				} else {
					minNum = 0;
				}
				for( int i=minNum; i<endNum; ++i ) {
					if( dest.addressesAllocated[i] == null ) {
						dest.addressesAllocated[i] = frame.sourceMacAddress;
						byte[] addr = new byte[16];
						for( int j=0; j<16; ++j ) addr[j] = dest.ip6Address[j];
						byte o = addr[dest.prefixLength/8];
						addr[dest.prefixLength/8] = (dest.prefixLength % 8 == 0) ? setHigh(o,i) : setLow(o,i);
						sendWireless( frEvt.getTimestamp(), dest, frame.sourceMacAddress, new AddressGivementPacket(addr, dest.prefixLength+4) );
						break;
					}
				}
			} else if( payload instanceof AddressGivementPacket ) {
				AddressGivementPacket adp = (AddressGivementPacket)payload;
				giveAddress( dest, frEvt.getTimestamp(), adp.address, adp.prefixLength );
			}
		}
	}

	public void giveAddress( Router r, long ts, byte[] address, int prefixLength ) throws Exception {
		if( r.ip6Address[0] == 0 || prefixLength < r.prefixLength ) {
			r.ip6Address = address;
			r.prefixLength = prefixLength;
			
			sendWireless( ts, r, BROADCAST_MAC_ADDRESS, new RouterWorld.AddressAnnouncementPacket( r.ip6Address, r.prefixLength, 4 ) );
		}
	}
}
