package togos.networkrts.experimental.netsim2;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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
		fillWith( rwp, 100 );
		fixFocus();
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
					new RouterWorld.AddressGivementPacket( new byte[]{ 0x20, 0x20, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 24 )
				)
			)
		);
	}
	
	public static void main( String[] args ) {
		final RouterWorldApplet rwa = new RouterWorldApplet();
		rwa.addKeyListener(new KeyListener() {
			@Override public void keyTyped( KeyEvent kevt ) {}
			@Override public void keyReleased( KeyEvent kevt ) {}
			@Override public void keyPressed( KeyEvent kevt ) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_PLUS ): case( KeyEvent.VK_EQUALS ):
					rwa.rwp.scale *= 1.25;
					break;
				case( KeyEvent.VK_MINUS ): case( KeyEvent.VK_UNDERSCORE ):
					rwa.rwp.scale /= 1.25;
					break;
				case( KeyEvent.VK_UP ):
					rwa.rwp.cy -= rwa.getHeight() / 4 / rwa.rwp.scale;
					break;
				case( KeyEvent.VK_DOWN ):
					rwa.rwp.cy += rwa.getHeight() / 4 / rwa.rwp.scale;
					break;
				case( KeyEvent.VK_LEFT ):
					rwa.rwp.cx -= rwa.getWidth() / 4 / rwa.rwp.scale;
					break;
				case( KeyEvent.VK_RIGHT ):
					rwa.rwp.cx += rwa.getWidth() / 4 / rwa.rwp.scale;
					break;
				}
			}
		});
		rwa.runWindowed();
	}
}
