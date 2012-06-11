package togos.networkrts.experimental.netsim2;

import java.awt.Color;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import togos.blob.util.BlobUtil;
import togos.networkrts.experimental.gensim.Timestamped;

public class RouterWorld implements EventHandler
{
	public static final int CHANNEL_1 = 0x01;
	public static final int CHANNEL_2 = 0x02;
	public static final int CHANNEL_3 = 0x04;
	
	static class TransmitterType {
		public final int channels;
		public final double power;
		public final Color color;
		
		public TransmitterType( int channels, double power, Color c ) {
			this.channels = channels;
			this.power = power;
			this.color = c;
		}
	}
	
	static TransmitterType[] BASIC_WIRELESS_TRANSMITTERS = new TransmitterType[] {
		new TransmitterType( CHANNEL_1, 100, Color.DARK_GRAY )
	};
	
	static TransmitterType[] LEVEL_2_WIRELESS_TRANSMITTERS = new TransmitterType[] {
		new TransmitterType( CHANNEL_1, 100, Color.DARK_GRAY ),
		new TransmitterType( CHANNEL_2, 500, Color.LIGHT_GRAY ),
	};
	
	static TransmitterType[] LEVEL_3_WIRELESS_TRANSMITTERS = new TransmitterType[] {
		new TransmitterType( CHANNEL_1,  100, Color.DARK_GRAY ),
		new TransmitterType( CHANNEL_2,  500, Color.LIGHT_GRAY ),
		new TransmitterType( CHANNEL_3, 1500, Color.RED ),
	};
	
	class Router {
		public final long id;
		public double x, y;
		public final byte[] macAddress;
		public final Set<Router> wireLinks = new HashSet<Router>();
		public byte[] ip6Address = new byte[16];
		public int ip6PrefixLength = 128;
		public int ip6ChildBits = 4;
		public byte[][] addressesAllocated = new byte[16][];
		
		public TransmitterType[] transmitters = BASIC_WIRELESS_TRANSMITTERS;
		public int receiveChannels = CHANNEL_1;
		public int type = 0;
		
		public Router( long id, byte[] macAddress ) {
			this.id = id;
			this.macAddress = macAddress;
		}

		public TransmitterType transmitterForChannel( int channel ) {
			for( TransmitterType tt : transmitters ) {
				if( (tt.channels & channel) != 0 ) return tt;
			}
			return null;
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
			r.x = rand.nextInt(2048);
			r.y = rand.nextInt(2048);
			if( rand.nextDouble() < 0.01 ) {
				r.transmitters = LEVEL_3_WIRELESS_TRANSMITTERS;
				r.receiveChannels = CHANNEL_1 | CHANNEL_2 | CHANNEL_3;
				r.type = 2;
			} else if( rand.nextDouble() < 0.05 ) {
				r.transmitters = LEVEL_2_WIRELESS_TRANSMITTERS;
				r.receiveChannels = CHANNEL_1 | CHANNEL_2;
				r.type = 1;
			}
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
		public final int channels;
		public final Frame data;
		
		public WirelessTransmissionEvent( double sx, double sy, long timestamp, double speed, double intensity, int channels, Frame data ) {
			this.sx = sx;
			this.sy = sy;
			this.time = timestamp;
			this.speed = speed;
			this.intensity = intensity;
			this.channels = channels; 
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
		public final int channel;
		public final Frame data;
		
		public FrameReceptionEvent( Router dest, long timestamp, int channel, Frame data ) {
			this.destination = dest;
			this.timestamp = timestamp;
			this.channel = channel;
			this.data = data;
		}
		
		@Override public long getTimestamp() {
			return timestamp;
		}
	}
	
	public static final byte[] BROADCAST_MAC_ADDRESS = new byte[]{-1,-1,-1,-1,-1,-1};
	
	protected void sendWireless( long ts, Router source, TransmitterType tt, byte[] destMac, Object payload ) throws Exception {
		if( tt == null ) return;
		eventScheduler.give( new WirelessTransmissionEvent( source.x, source.y, ts, c,
			tt.power, tt.channels,
			new RouterWorld.Frame( source.macAddress, destMac, payload )
		));
	}

	protected void sendWireless( long ts, Router source, byte[] destMac, Object payload ) throws Exception {
		for( TransmitterType tt : source.transmitters ) {
			sendWireless( ts, source, tt, destMac, payload );
		}
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
					if( (r.receiveChannels & wtEvt.channels) == 0 ) continue;
					double dx = r.x - wtEvt.sx;
					double dy = r.y - wtEvt.sy;
					double dist = Math.sqrt(dx*dx+dy*dy);
					double interval = dist / wtEvt.speed;
					long receptionTime = wtEvt.getTimestamp() + (long)(interval*1000);
					double receptionIntensity = wtEvt.getIntensity( receptionTime );
					if( dist > 0 && receptionIntensity >= 1 ) {
						eventScheduler.give( new FrameReceptionEvent(r, receptionTime, wtEvt.channels, wtEvt.data) );
					}
				}
			}
		} else if( evt instanceof FrameReceptionEvent ) {
			FrameReceptionEvent frEvt = (FrameReceptionEvent)evt;
			Frame frame = frEvt.data;
			Router dest = frEvt.destination;
			Object payload = frame.payload;
			TransmitterType tt = dest.transmitterForChannel(frEvt.channel);

			if( !BlobUtil.equals(BROADCAST_MAC_ADDRESS,frame.destMacAddress) && !BlobUtil.equals(frEvt.destination.macAddress, frame.destMacAddress) ) {
				return;
			}
			
			if( payload instanceof AddressAnnouncementPacket ) {
				AddressAnnouncementPacket aap = (AddressAnnouncementPacket)payload;
				if( dest.ip6Address[0] == 0 || dest.ip6PrefixLength > aap.prefixLength+aap.childBits ) {
					// Request an address from the sender
					sendWireless( frEvt.getTimestamp(), dest, tt, frame.sourceMacAddress, new RouterWorld.AddressRequestPacket() );
				}
			} else if( payload instanceof AddressRequestPacket ) {
				if( dest.ip6PrefixLength + dest.ip6ChildBits > 128 ) return;
				int minNum, endNum=15;
				if( dest.ip6PrefixLength + dest.ip6ChildBits == 128 ) {
					minNum = 2;
				} else {
					minNum = 1;
				}
				for( int i=minNum; i<endNum; ++i ) {
					if( dest.addressesAllocated[i] == null ) {
						dest.addressesAllocated[i] = frame.sourceMacAddress;
						byte[] addr = new byte[16];
						for( int j=0; j<16; ++j ) addr[j] = dest.ip6Address[j];
						byte o = addr[dest.ip6PrefixLength/8];
						addr[dest.ip6PrefixLength/8] = (dest.ip6PrefixLength % 8 == 0) ? setHigh(o,i) : setLow(o,i);
						sendWireless( frEvt.getTimestamp(), dest, tt, frame.sourceMacAddress, new AddressGivementPacket(addr, dest.ip6PrefixLength+dest.ip6ChildBits) );
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
		if( r.ip6Address[0] == 0 || prefixLength < r.ip6PrefixLength ) {
			r.ip6Address = address;
			r.ip6PrefixLength = prefixLength;
			
			sendWireless( ts, r, BROADCAST_MAC_ADDRESS, new RouterWorld.AddressAnnouncementPacket( r.ip6Address, r.ip6PrefixLength, r.ip6ChildBits ) );
		}
	}
}
