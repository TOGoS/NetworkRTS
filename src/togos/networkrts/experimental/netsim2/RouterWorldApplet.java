package togos.networkrts.experimental.netsim2;

import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.KeyAdapter;
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
		Router r = rw.randomRouter();
		for( int i=0; r != null && r.ip6Address[0] == 0 && i<100; ++i ) {
			r = rw.randomRouter();
		}
		return r;
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
		
		final Simulator simulator = new Simulator();
		simulator.teq = new TimedEventQueue<Timestamped>();
		rw.eventScheduler = simulator.teq;
		simulator.teq.advanceTimeTo( System.currentTimeMillis() );
		rw.init();
		
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
		
		// Initialize UI
		
		fillWith( rwp, 33 );
		
		addKeyListenerEverywhere(new KeyListener() {
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
				case( KeyEvent.VK_L ):
					rwp.drawLinks ^= true;
					break;
				case( KeyEvent.VK_R ):
					rw.clear();
					rw.init();
					rw.beginAddressAllocation(simulator.teq.getCurrentTimestamp());
					break;
				case( KeyEvent.VK_F1 ):
					TextArea helpTextArea = new TextArea(
						"Welcome to TOGoS's wireless network simulator!\n" +
						"\n" +
						"Controls:\n" +
						"Arrow keys move the camera.\n" +
						"+/- zoom in and out.\n" +
						"P pings a random node from another.\n" +
						"R randomizes the world.\n" +
						"\n" +
						"About:\n" +
						"Demonstrates a simple algorithm for allocating\n" +
						"IPv6 addresses and routing packets in a random network.\n" +
						"There are 3 types of wireless routers which can transmit\n" +
						"over varying distances.  Gray, white, and red lines\n" +
						"represent possible short, medium, and long-range links.\n" +
						"\n" +
						"Transmission colors:\n" +
						"Green are address announcements.\n" +
						"Orange are address requests.\n" +
						"Yellow are address provisions.\n" +
						"White are routable packets (pings, pongs).", 
						30, 60, TextArea.SCROLLBARS_BOTH
					);
					helpTextArea.setEditable(false);
					final Frame helpFrame = new Frame("RouterWorldApplet help");
					helpTextArea.addKeyListener( new KeyAdapter() {
						public void keyPressed(KeyEvent e) {
							if( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
								helpFrame.dispose();
							}
						};
					});
					helpFrame.add(helpTextArea);
					helpTextArea.requestFocus();
					helpFrame.pack();
					helpFrame.addWindowListener( new WindowAdapter() {
						@Override public void windowClosing(WindowEvent e) {
							helpFrame.dispose();
						}
					});
					helpFrame.setVisible(true);
				}
			}
		});
		
		rw.beginAddressAllocation( simulator.teq.getCurrentTimestamp() );
	}
	
	public void start() {
		setFocusable(true);
		requestFocus();
	}
	
	public static void main( String[] args ) {
		new RouterWorldApplet().runWindowed();
	}
}
