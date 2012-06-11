package togos.networkrts.experimental.netsim2;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import togos.networkrts.awt.Apallit;
import togos.networkrts.experimental.gensim.Simulator;
import togos.networkrts.experimental.gensim.TimedEventQueue;
import togos.networkrts.experimental.gensim.Timestamped;
import togos.networkrts.experimental.netsim2.RouterWorld.Router;
import togos.service.InterruptableSingleThreadedService;

public class RouterWorldApplet extends Apallit
{
	private static final long serialVersionUID = -378767516226959431L;

	RouterWorld rw = new RouterWorld();
	RouterWorldPaintable rwp = new RouterWorldPaintable(rw);
	
	public void init() {
		setTitle("Router World");
		super.init();
		
		addKeyListener(new KeyListener() {
			@Override public void keyTyped( KeyEvent kevt ) {}
			@Override public void keyReleased( KeyEvent kevt ) {}
			@Override public void keyPressed( KeyEvent kevt ) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_PLUS ): case( KeyEvent.VK_EQUALS ):
					rwp.scale *= 1.25;
					break;
				case( KeyEvent.VK_MINUS ): case( KeyEvent.VK_UNDERSCORE ):
					rwp.scale /= 1.25;
					break;
				case( KeyEvent.VK_UP ):
					rwp.cy -= getHeight() / 4 / rwp.scale;
					break;
				case( KeyEvent.VK_DOWN ):
					rwp.cy += getHeight() / 4 / rwp.scale;
					break;
				case( KeyEvent.VK_LEFT ):
					rwp.cx -= getWidth() / 4 / rwp.scale;
					break;
				case( KeyEvent.VK_RIGHT ):
					rwp.cx += getWidth() / 4 / rwp.scale;
					break;
				}
			}
		});
		
		final Simulator simulator = new Simulator();
		simulator.teq = new TimedEventQueue<Timestamped>();
		rw.eventScheduler = simulator.teq;
		simulator.teq.advanceTimeTo( System.currentTimeMillis() );
		rw.initRouters(512);
		rwp.cx = 512;
		rwp.cy = 512;
		fillWith( rwp, 33 );
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
		Router rootRouter = rw.routers.iterator().next();
		try {
			rw.giveAddress( rootRouter, simulator.teq.getCurrentTimestamp(), new byte[]{0x20,0x20,0,0,0,0,0,0,0x12,0x34,0,0,0,0,0,1}, 80 );
		} catch( Exception e ) {
			throw new RuntimeException(e);
		}
		validate(); // Do it again?
	}
	
	public static void main( String[] args ) {
		new RouterWorldApplet().runWindowed();
	}
}
