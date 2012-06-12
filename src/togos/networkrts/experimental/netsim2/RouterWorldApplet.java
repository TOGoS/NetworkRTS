package togos.networkrts.experimental.netsim2;

import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

import togos.networkrts.awt.Apallit;
import togos.networkrts.experimental.gensim.Simulator;
import togos.networkrts.experimental.gensim.TimedEventQueue;
import togos.networkrts.experimental.gensim.Timestamped;
import togos.networkrts.experimental.netsim2.RouterWorld.Router;
import togos.networkrts.inet.AddressUtil;
import togos.service.InterruptableSingleThreadedService;

public class RouterWorldApplet extends Apallit
{
	private static final long serialVersionUID = -378767516226959431L;

	RouterWorld rw = new RouterWorld();
	RouterWorldPaintable rwp = new RouterWorldPaintable(rw);
	
	Random rand = new Random();
	protected Router pickRouterWithIp6Address() {
		Router[] routers = rw.routers.toArray(new Router[rw.routers.size()]);
		for( int i=0; i<100; ++i ) {
			Router r = routers[rand.nextInt(routers.length)];
			if( r.ip6Address[0] != 0 ) return r;
		}
		return routers[rand.nextInt(routers.length)];
	}
	
	protected void ping() throws Exception {
		Router source = pickRouterWithIp6Address();
		Router dest = pickRouterWithIp6Address();
		rwp.statusText = "Ping from "+AddressUtil.formatIp6Address(source.ip6Address,0)+" to "+AddressUtil.formatIp6Address(dest.ip6Address,0);
		rw.ping( source, System.currentTimeMillis(), dest.ip6Address );
	}
	
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
				case( KeyEvent.VK_P ):
					try {
						ping();
					} catch( Exception e ) {
						e.printStackTrace();
					}
					break;
				case( KeyEvent.VK_F1 ):
					TextArea helpTextArea = new TextArea(
						"Welcome to TOGoS's wireless network simulator!\n" +
						"\n" +
						"Use arrow keys to move the camera and +/- to zoom.\n" +
						"Press 'P' to have a random node ping another.\n",
						10, 40, TextArea.SCROLLBARS_BOTH
					);
					final Frame helpFrame = new Frame("RouterWorldApplet help");
					helpFrame.add(helpTextArea);
					helpFrame.pack();
					helpFrame.setVisible(true);
					helpFrame.addWindowListener( new WindowAdapter() {
						@Override public void windowClosing(WindowEvent e) {
							helpFrame.dispose();
						}
					});
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
