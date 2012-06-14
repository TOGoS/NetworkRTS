package togos.networkrts.experimental.netsim2;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import togos.blob.util.BlobUtil;
import togos.networkrts.experimental.entree.ClipShape;
import togos.networkrts.experimental.entree.TouchyClipRectangle;
import togos.networkrts.experimental.entree2.Entree;
import togos.networkrts.experimental.entree2.QuadEntree;
import togos.networkrts.experimental.entree2.QuadEntreeNode;
import togos.networkrts.experimental.entree2.WorldObject;
import togos.networkrts.experimental.entree2.WorldUpdateBuilder;
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
	
	class Router extends WorldObject {
		public boolean alive;
		public final byte[] macAddress;
		public byte[] ip6Address = new byte[16];
		public int ip6PrefixLength = 128;
		public int ip6ChildBits = 4;
		public byte[][] addressesAllocated = new byte[16][];
		public List<Neighbor> neighbors = new ArrayList<Neighbor>();
		
		public TransmitterType[] transmitters = BASIC_WIRELESS_TRANSMITTERS;
		public int receiveChannels = CHANNEL_1;
		public int type = 0;
		
		public Router( double x, double y, byte[] macAddress ) {
			super( x, y );
			this.macAddress = macAddress;
		}
		
		public TransmitterType transmitterForChannel( int channel ) {
			for( TransmitterType tt : transmitters ) {
				if( (tt.channels & channel) != 0 ) return tt;
			}
			return null;
		}
		
		@Override public double getMaxRadius() {
			return 2;
		}
		
		@Override public long getFlags() { return 0; }
		@Override public long getAutoUpdateTime() { return 0; }
	}
	
	public Entree<Router> routerEntree = new QuadEntree<Router>( -32768, -32768, 65536, 65536, QuadEntreeNode.EMPTY, 11 );
	
	public Sink<Timestamped> eventScheduler;
	double c = 300;
	public int pingsSent, pongsSent, pongsReceived;
	
	Random rand = new Random();
	
	protected synchronized byte[] newMacAddress() {
		byte[] mac = new byte[6];
		rand.nextBytes(mac);
		return mac;
	}
	
	protected void setRouterType( Router r, int type ) {
		switch( type ) {
		case( 3 ):
			r.transmitters = LEVEL_3_WIRELESS_TRANSMITTERS;
			r.receiveChannels = CHANNEL_1 | CHANNEL_2 | CHANNEL_3;
			r.type = 3;
			break;
		case( 2 ):
			r.transmitters = LEVEL_2_WIRELESS_TRANSMITTERS;
			r.receiveChannels = CHANNEL_1 | CHANNEL_2;
			r.type = 2;
			break;
		case( 1 ):
			r.transmitters = BASIC_WIRELESS_TRANSMITTERS;
			r.receiveChannels = CHANNEL_1;
			r.type = 2;
		}
	}
	
	protected void addRouter( double x, double y, int type, WorldUpdateBuilder ub ) {
		Router r = new Router( x, y, newMacAddress() );
		setRouterType(r, type);
		r.alive = true;
		ub.add(r);
	}
	
	protected void initRandomRouters( int count, int size, WorldUpdateBuilder ub ) {
		for( ; count > 0; --count ) {
			int type;
			if( rand.nextDouble() < 0.01 ) {
				type = 3;
			} else if( rand.nextDouble() < 0.05 ) {
				type = 2;
			} else {
				type = 1;
			}
			addRouter( rand.nextInt(size)-size/2, rand.nextInt(size)-size/2, type, ub );
		}
	}
	
	protected void addLevel1Tree( double cx, double cy, WorldUpdateBuilder ub ) {
		int count = rand.nextInt(50);
		for( int i=0; i<count; ++i ) {
			addRouter( cx+rand.nextDouble()*400-200, cy+rand.nextDouble()*400-200, 1, ub );
		}
	}
	
	protected void addLevel2Cluster( double cx, double cy, WorldUpdateBuilder ub ) {
		addRouter( cx, cy, 2, ub );
		addLevel1Tree( cx, cy, ub );
	}
	
	protected void addLevel2Tree( double cx, double cy, WorldUpdateBuilder ub ) {
		switch( rand.nextInt(4) ) {
		case( 0 ):
			// ring!
			double rad = 250+rand.nextDouble()*500;
			double cir = rad * 2 * Math.PI;
			int count = (int)(cir / 500)+1;
			double phase = rand.nextDouble();
			for( int i=0; i<count; ++i ) {
				double o = i*2*Math.PI/count + phase;
				addLevel2Cluster( cx+rad*Math.cos(o), cy+rad*Math.sin(o), ub );
			}
			break;
		case( 1 ):
			// star!
			addLevel2Cluster( cx, cy, ub );
			addLevel2Cluster( cx - 300, cy - 400, ub );
			addLevel2Cluster( cx + 300, cy + 400, ub );
			addLevel2Cluster( cx - 400, cy + 300, ub );
			addLevel2Cluster( cx + 400, cy - 300, ub );
			break;
		case( 2 ):
			// line!
			double ang = rand.nextDouble()*Math.PI*2;
			for( int i=rand.nextInt(7); i>=0; --i ) {
				cx += 450*Math.cos(ang);
				cy += 450*Math.sin(ang);
				addLevel2Cluster( cx, cy, ub );
				ang += rand.nextGaussian();
			}
			break;
		case( 3 ):
			// random!
			for( int i=rand.nextInt(7); i>=0; --i ) {
				addLevel2Cluster( cx + rand.nextDouble()*1024-512, cy + rand.nextDouble()*1024-512, ub );
			}
		}
	}
	
	protected void addLevel3Cluster( double cx, double cy, WorldUpdateBuilder ub ) {
		addRouter( cx, cy, 3, ub );
		addLevel2Tree( cx, cy, ub );
	}
	
	protected void initLevel3Tree( double cx, double cy, WorldUpdateBuilder ub ) {
		switch( rand.nextInt(4) ) {
		case( 0 ):
			// ring!
			double rad = 1024+rand.nextDouble()*2048;
			double cir = rad * 2 * Math.PI;
			int count = (int)(cir / 1500)+1;
			for( int i=0; i<count; ++i ) {
				addLevel3Cluster( cx+rad*Math.cos(i*2*Math.PI/count), cy+rad*Math.sin(i*2*Math.PI/count), ub );
			}
			break;
		case( 1 ):
			// star!
			addLevel3Cluster( cx, cy, ub );
			addLevel3Cluster( cx - 1500, cy, ub );
			addLevel3Cluster( cx + 1500, cy, ub );
			addLevel3Cluster( cx, cy - 1500, ub );
			addLevel3Cluster( cx, cy + 1500, ub );
			break;
		case( 2 ):
			// line!
			double ang = rand.nextDouble()*Math.PI*2;
			for( int i=rand.nextInt(7); i>=0; --i ) {
				cx += 1400*Math.cos(ang);
				cy += 1400*Math.sin(ang);
				addLevel3Cluster( cx, cy, ub );
				ang += rand.nextGaussian();
			}
			break;
		case( 3 ):
			// random!
			for( int i=rand.nextInt(7); i>=0; --i ) {
				addLevel2Cluster( cx + rand.nextDouble()*4096-2048, cy + rand.nextDouble()*4096-2048, ub );
			}
		}
	}
	
	public void init() {
		WorldUpdateBuilder ub = new WorldUpdateBuilder();
		switch( rand.nextInt(2) ) {
		case( 0 ):
			int scale = 1+rand.nextInt(7);
			initRandomRouters( 768*scale*scale, 2048*scale, ub );
			break;
		case( 1 ):
			initLevel3Tree( 0, 0, ub );
			break;
		}
		routerEntree = ub.applyAndClear(routerEntree);
	}
	
	public Router randomRouter() {
		class PickedOne extends Exception {
			private static final long serialVersionUID = 1L;
			public final Router r;
			public PickedOne( Router r ) {
				this.r = r;
			}
		}
		try {
			routerEntree.forEachObject(0, Long.MAX_VALUE, new ClipShape() {
				public boolean intersectsRect(double x, double y, double w, double h) { return true; }
			}, new Sink<RouterWorld.Router>() {
				int num = rand.nextInt(routerEntree.getObjectCount());
				
				@Override public void give(Router r) throws Exception {
					if( num == 0 ) throw new PickedOne(r);
					--num;
				}
			});
		} catch( PickedOne p1 ) {
			return p1.r;
		} catch( Exception e ) {
			throw new RuntimeException(e);
		}
		return null;
	}
	
	public void beginAddressAllocation( long timestamp ) {
		Router rootRouter = randomRouter();
		if( rootRouter == null ) return;
		try {
			giveAddress( rootRouter, timestamp, new byte[]{0x20,0x20,0,0,0,0,0,0,0x12,0x34,0,0,0,0,0,1}, 80 );
		} catch( Exception e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void clear() {
		try {
			routerEntree.forEachObject(0,  Long.MAX_VALUE, new ClipShape() {
				public boolean intersectsRect(double x, double y, double w, double h) { return true; }
			}, new Sink<Router>() {
				@Override public void give(Router r) throws Exception {
					r.alive = false;
				}
			});
		} catch( Exception e ) {
			throw new RuntimeException(e);
		}
		routerEntree = new QuadEntree<Router>( -32768, -32768, 65536, 65536, QuadEntreeNode.EMPTY, 11 );
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
	
	static class DestinationPacket implements Cloneable {
		public final byte[] destAddress;
		protected int ttl;
		
		public DestinationPacket( byte[] dest ) {
			this.destAddress = dest;
			this.ttl = 64;
		}
		
		public int getTtl() {  return ttl;  }
		
		public DestinationPacket decrTtl() {
			try {
				DestinationPacket c = (DestinationPacket)clone();
				--c.ttl;
				return c;
			} catch( CloneNotSupportedException e ) {
				throw new RuntimeException(e);
			}
		}
	}
	
	static class SourceDestPacket extends DestinationPacket {
		public final byte[] sourceAddress;
		
		public SourceDestPacket( byte[] s, byte[] d ) {
			super(d);
			this.sourceAddress = s;
		}
	}
	
	static class PingPacket extends SourceDestPacket {
		public final byte[] data;
		
		public PingPacket( byte[] s, byte[] d, byte[] a ) {
			super( s, d );
			this.data = a;
		}
	}
	
	static class PongPacket extends SourceDestPacket {
		public final byte[] data;
		
		public PongPacket( byte[] s, byte[] d, byte[] a ) {
			super( s, d );
			this.data = a;
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
		public final double intensity; // at radius = 1; also = distance it will travel
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
	
	protected static int addressBit( byte[] addy, int bit ) {
		return (addy[bit/8] >> (7-(bit%8))) & 1;
	}
	
	static class Neighbor {
		byte[] macAddress;
		int prefixLength;
		byte[] ip6address;
		int channel;
		
		public Neighbor( byte[] macAddress, int channel, byte[] ip6Address, int prefixLength ) {
			this.macAddress = macAddress;
			this.channel = channel;
			this.ip6address = ip6Address;
			this.prefixLength = prefixLength;
		}
		
		public boolean contains( byte[] address ) {
			for( int i=0; i<prefixLength; ++i ) {
				if( addressBit(address,i) != addressBit(ip6address,i) ) return false;
			}
			return true;
		}
	}
	
	protected void forward( Router source, long timestamp, DestinationPacket packet ) throws Exception {
		// Forward it!
		Neighbor nearest = null;
		Neighbor shortest = null;
		
		int longestContainingPrefix = 0;
		int shortestPrefix = 128;
		for( Neighbor n : source.neighbors ) {
			if( n.contains(packet.destAddress) && n.prefixLength > longestContainingPrefix ) {
				nearest = n;
				longestContainingPrefix = n.prefixLength;
			}
			if( n.prefixLength < shortestPrefix ) {
				shortestPrefix = n.prefixLength;
				shortest = n;
			}
		}
		
		Neighbor next = nearest != null ? nearest : shortest;
		
		if( next != null ) {
			TransmitterType tt = source.transmitterForChannel(next.channel);
			sendWireless( timestamp, source, tt, next.macAddress, packet );
		}
	}
	
	@Override public void eventOccured( final Timestamped evt ) throws Exception {
		if( evt instanceof WirelessTransmissionEvent ) {
			final WirelessTransmissionEvent wtEvt = (WirelessTransmissionEvent)evt;
			double intensity = wtEvt.intensity+1;
			if( wtEvt.data instanceof Frame ) {
				routerEntree.forEachObject(0, Long.MAX_VALUE, new TouchyClipRectangle(wtEvt.sx-intensity, wtEvt.sy-intensity, intensity*2, intensity*2), new Sink<RouterWorld.Router>() {
					@Override public void give(Router r) throws Exception {
						// TODO: could use flags to indicate channels and possibly save some time
						if( (r.receiveChannels & wtEvt.channels) == 0 ) return;
						
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
				});
			}
		} else if( evt instanceof FrameReceptionEvent ) {
			FrameReceptionEvent frEvt = (FrameReceptionEvent)evt;
			Frame frame = frEvt.data;
			Router dest = frEvt.destination;
			Object payload = frame.payload;
			
			if( !dest.alive ) {
				return;
			}

			if( !BlobUtil.equals(BROADCAST_MAC_ADDRESS,frame.destMacAddress) && !BlobUtil.equals(frEvt.destination.macAddress, frame.destMacAddress) ) {
				return;
			}
			
			if( payload instanceof DestinationPacket ) {
				DestinationPacket destPack = (DestinationPacket)payload;
				
				if( BlobUtil.equals( destPack.destAddress, dest.ip6Address) ) {
					if( payload instanceof PingPacket ) {
						PingPacket ping = (PingPacket)payload;
						forward( dest, frEvt.getTimestamp(), new PongPacket(ping.destAddress, ping.sourceAddress, ping.data) );
						++pongsSent;
					} else if( payload instanceof PongPacket ) {
						++pongsReceived;
					}
				} else if( destPack.getTtl() > 0 ) {
					forward( dest, frEvt.getTimestamp(), destPack.decrTtl() );
				}
			} else if( payload instanceof AddressAnnouncementPacket ) {
				AddressAnnouncementPacket aap = (AddressAnnouncementPacket)payload;
				if( dest.ip6Address[0] == 0 || dest.ip6PrefixLength > aap.prefixLength+aap.childBits ) {
					// Request an address from the sender
					TransmitterType tt = dest.transmitterForChannel(frEvt.channel);
					sendWireless( frEvt.getTimestamp(), dest, tt, frame.sourceMacAddress, new RouterWorld.AddressRequestPacket() );
				}
				for( Iterator<Neighbor> ni = dest.neighbors.iterator(); ni.hasNext(); ) {
					if( BlobUtil.equals( frame.sourceMacAddress, ni.next().macAddress ) ) {
						ni.remove();
					}
				}
				dest.neighbors.add( new Neighbor( frame.sourceMacAddress, frEvt.channel, aap.address, aap.prefixLength ) );
			} else if( payload instanceof AddressRequestPacket ) {
				TransmitterType tt = dest.transmitterForChannel(frEvt.channel);

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
	
	public void ping( Router source, long ts, byte[] destAddress ) throws Exception {
		byte[] data = new byte[32];
		rand.nextBytes(data);
		forward(source, System.currentTimeMillis(), new RouterWorld.PingPacket( source.ip6Address, destAddress, data ));
		++pingsSent;
	}
}
