package togos.networkrts.experimental.netsim2;

import togos.networkrts.awt.Apallit;
import togos.networkrts.experimental.gensim.Simulator;
import togos.networkrts.experimental.gensim.TimedEventQueue;
import togos.networkrts.experimental.gensim.Timestamped;
import togos.service.InterruptableSingleThreadedService;

public class RouterWorldApplet extends Apallit
{
	private static final long serialVersionUID = -378767516226959431L;

	RouterWorld rw = new RouterWorld();
	RouterWorldPaintable rwp = new RouterWorldPaintable(rw);
	
	public void init() {
		setTitle("Router World");
		super.init();
		final Simulator simulator = new Simulator();
		simulator.teq = new TimedEventQueue<Timestamped>();
		rw.eventScheduler = simulator.teq;
		simulator.teq.advanceTimeTo( System.currentTimeMillis() );
		rw.initRouters(150);
		rwp.cx = 512;
		rwp.cy = 512;
		fillWith( rwp, 768, 512, 100 );
		addService( new InterruptableSingleThreadedService() {
			@Override protected void _run() throws InterruptedException {
				try {
					simulator.run();
				} catch( InterruptedException e ) {
					throw e;
				} catch( Exception e ) {
					e.printStackTrace();
				}
			}
		});
		addService( new InterruptableSingleThreadedService() {
			@Override protected void _run() throws InterruptedException {
				try {
					while( true ) {
						Thread.sleep(10);
						simulator.teq.advanceTimeTo(System.currentTimeMillis());
					}
				} catch( InterruptedException e ) {
					throw e;
				} catch( Exception e ) {
					e.printStackTrace();
				}
			}
		});
		simulator.eventHandler = new EventHandler() {
			@Override public void eventOccured(Timestamped event) throws Exception {
				rwp.eventOccured( event );
				rw.eventOccured( event );
			}
		};
		simulator.teq.give(
			new RouterWorld.WirelessTransmissionEvent(
				512, 512, simulator.teq.getCurrentTimestamp(), rw.c, rw.normalTransmissionIntensity,
				new RouterWorld.Frame( new byte[]{0,0,0,0,0,0}, RouterWorld.BROADCAST_MAC_ADDRESS,
					new RouterWorld.AddressAnnouncementPacket( new byte[]{ 0x20, 0x20, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 24 )
				)
			)
		);
	}
	
	public static void main( String[] args ) {
		RouterWorldApplet rwa = new RouterWorldApplet();
		rwa.runWindowed();
	}
}
