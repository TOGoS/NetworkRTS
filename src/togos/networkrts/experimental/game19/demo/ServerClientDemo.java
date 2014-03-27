package togos.networkrts.experimental.game19.demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import togos.networkrts.experimental.game19.Renderer;
import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.scene.Layer.VisibilityClip;
import togos.networkrts.experimental.game19.sim.Simulator;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.IDGenerator;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManBehavior;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManCoreStats;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManIcons;
import togos.networkrts.ui.ImageCanvas;

public class ServerClientDemo
{
	// TODO: Move cell visibility into Scene
	//   (so it can be drawn over everything, including sprites)
	
	/**
	 * A scene represents a portion of the world.
	 * The primary use for a scene is to send the part of the world
	 * that a character can see to a client to display.
	 */
	public static class Scene {
		public final Layer layer;
		public final List<NonTile> nonTiles;
		// Point within the scene that should be centered on (usually the player)
		public final double poiX, poiY;
		/**
		 * Section of the scene that is visible
		 * (offsets are relative to the layer's origin)
		 **/
		public final VisibilityClip visibilityClip;
		
		protected static final Comparator<NonTile> NONTILE_COMPARATOR = new Comparator<NonTile>() {
			public int compare(NonTile arg0, NonTile arg1) {
				float z0 = arg0.getIcon().imageZ, z1 = arg1.getIcon().imageZ;  
				return z0 < z1 ? -1 : z0 > z1 ? 1 : 0;
			}
		};
		
		public Scene( Layer layer,  List<NonTile> nonTiles, double poiX, double poiY, VisibilityClip visibilityClip ) {
			this.layer = layer;
			Collections.sort(nonTiles, NONTILE_COMPARATOR);
			this.nonTiles = nonTiles;
			this.poiX = poiX;
			this.poiY = poiY;
			this.visibilityClip = visibilityClip;
		}
	}
	
	public static class UIState {
		public final Scene scene;
		public final JetManCoreStats stats;
		public final boolean connected;
		
		public UIState( Scene scene, JetManCoreStats stats, boolean connected ) {
			this.scene = scene;
			this.stats = stats;
			this.connected = connected;
		}
	}
	
	static class SceneCanvas extends ImageCanvas {
		private static final long serialVersionUID = 1L;

		protected BufferedImage sceneBuffer; // = new BufferedImage(512, 384, BufferedImage.TYPE_INT_RGB); // Much faster than ARGB!
		protected synchronized BufferedImage getSceneBuffer( int width, int height ) {
			if( sceneBuffer == null || sceneBuffer.getWidth() != width || sceneBuffer.getHeight() != height ) {
				return sceneBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			} else {
				return sceneBuffer;
			}
		}
		
		int pixelsPerMeter = 16;
		
		protected final Renderer renderer;
		public SceneCanvas( ResourceContext resourceContext ) {
			renderer = new Renderer(resourceContext);
		}
		
		Color sceneBackgroundColor = new Color(0.2f, 0, 0);
		UIState uiState = new UIState(null, null, false);
		
		public synchronized void setUiState( UIState s ) {
			this.uiState = s;
			redrawBuffer();
		}
		
		public synchronized void zoomMore() {
			if( pixelsPerMeter < 128 ) {
				pixelsPerMeter <<= 1;
				redrawBuffer();
			}
		}
		
		public synchronized void zoomLess() {
			if( pixelsPerMeter > 1 ) {
				pixelsPerMeter >>= 1;
				redrawBuffer();
			}
		}
		
		protected int roundEven(double v) {
			return 2*(int)Math.round(v/2);
		}
		
		protected boolean needRedraw = true;
		protected synchronized void redrawBuffer() {
			needRedraw = true;
			notifyAll();
		}
		
		public void redrawLoop() throws InterruptedException {
			UIState u = null;
			while( true ) {
				synchronized(this) {
					while( !needRedraw || uiState == null ) wait();
					u = uiState;
				}
				Scene scene = u.scene;
				int wid, hei;
				if( scene != null ) {
					VisibilityClip vc = scene.visibilityClip;
					int vcWidth  = roundEven(pixelsPerMeter*(vc.maxX-vc.minX));
					int vcHeight = roundEven(pixelsPerMeter*(vc.maxY-vc.minY));
					wid = Math.min(vcWidth, getWidth());
					hei = Math.min(vcHeight,getHeight());
				} else {
					wid = getWidth();
					hei = getHeight();
				}
				BufferedImage sb = getSceneBuffer(wid, hei);
				synchronized( sb ) {
					Graphics g = sb.getGraphics();
					g.setClip(0, 0, sb.getWidth(), sceneBuffer.getHeight());
					g.setColor( sceneBackgroundColor );
					g.fillRect( 0, 0, sb.getWidth(), sb.getHeight() );
					if( scene != null ) {
						renderer.draw( scene, -scene.poiX, -scene.poiY, 1, g, pixelsPerMeter, sb.getWidth()/2, sb.getHeight()/2 );
					}
					JetManCoreStats stats = u.stats;
					if( stats != null ) {
						g.setColor(Color.WHITE);
						g.drawString( String.format("Fuel: % 5.2f / % 5.2f", stats.fuel, stats.maxFuel), 4, 12);
						g.drawString( String.format("Suit: % 5.4f / % 5.4f", stats.suitHealth, stats.maxSuitHealth), 4, 12*2);
						g.drawString( String.format("Head: % 5.4f / % 5.4f", stats.helmetHealth, stats.maxHelmetHealth), 4, 12*3);
						g.drawString( String.format("Batt: % 5.4f / % 5.4f", stats.batteryCharge, stats.maxBatteryCharge), 4, 12*4);
					}
					if( !u.connected ) {
						Font f = new Font("Verdana", Font.PLAIN, 24);
						g.setFont(f);
						FontMetrics fm = g.getFontMetrics();
						String ns = "NO SIGNAL";
						
						int w = fm.stringWidth(ns);
						int asc = fm.getAscent();
						int desc = fm.getDescent();
						
						int rectHeight = asc+desc+8;
						int rectWidth = w+16;
						
						g.setColor(Color.RED);
						g.fillRect( (wid-rectWidth)/2, (hei-rectHeight)/2, rectWidth, rectHeight );
						g.setColor(Color.BLACK);
						g.drawRect( (wid-rectWidth)/2, (hei-rectHeight)/2, rectWidth, rectHeight );
						g.setColor(Color.WHITE);
						g.drawString( ns, (wid-w)/2, (hei-rectHeight)/2+4+asc );
					}
				}
				setImage(sb);
			}
		}
	}
	
	static class Client {
		SceneCanvas sceneCanvas;
		public Queue<Message> messageQueue;
		protected Scene scene;
		protected JetManCoreStats stats;
		public long lastUpdateFromAvatar;
		
		public Client( ResourceContext resourceContext ) {
			sceneCanvas = new SceneCanvas(resourceContext);
			sceneCanvas.setBackground(Color.BLACK);
		}
		
		protected void updateUiState() {
			sceneCanvas.setUiState(new UIState(scene, stats, lastUpdateFromAvatar >= System.currentTimeMillis() - 1000));
		}
		
		public synchronized void updateReceived() {
			lastUpdateFromAvatar = System.currentTimeMillis();
		}
		public synchronized void setScene( Scene s ) {
			scene = s;
			updateUiState();
		}
		public synchronized void setStats( JetManCoreStats s ) {
			stats = s;
			updateUiState();
		}
		
		public synchronized void pokeWatchdog() {
			long currentTime = System.currentTimeMillis();
			if( lastUpdateFromAvatar < currentTime - 1000 ) {
				updateUiState();
			}
		}
		
		public void startUi() {
			final Frame f = new Frame("Game19 Render Demo");
			f.add(sceneCanvas);
			final Thread watchdogThread = new Thread() {
				@Override public void run() {
					while( !Thread.interrupted() ) {
						pokeWatchdog();
						try {
							sleep(500);
						} catch( InterruptedException e ) {
							interrupt();
						}
					}
				}
			};
			final Thread redrawThread = new Thread() {
				@Override public void run() {
					try {
						sceneCanvas.redrawLoop();
					} catch( InterruptedException e ) {
						interrupt();
					}
				}
			};
			f.addWindowListener(new WindowAdapter() {
				@Override public void windowClosing(WindowEvent evt) {
					f.dispose();
					redrawThread.interrupt();
					// could shut down server nicely and stuff
					System.exit(0);
				}
			});
			f.pack();
			f.setVisible(true);
			redrawThread.start();
			watchdogThread.start();
		}
	}
	
	public static void main( String[] args ) throws Exception {
		final LinkedBlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>(); 
		
		IDGenerator idGenerator = new IDGenerator();
		
		final ResourceContext resourceContext = new ResourceContext(new File(".ccouch"));
		final Client c = new Client(resourceContext);
		final int playerId = idGenerator.newId();
		final int clientId = idGenerator.newId();
		final long clientBa = BitAddresses.forceType(BitAddresses.TYPE_EXTERNAL, clientId);
		c.startUi();
		c.sceneCanvas.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent kevt) {
				switch( kevt.getKeyCode() ) {
				case KeyEvent.VK_EQUALS: c.sceneCanvas.zoomMore(); break;
				case KeyEvent.VK_MINUS: c.sceneCanvas.zoomLess(); break;
				}
			}
		});
		c.sceneCanvas.addKeyListener(new KeyListener() {
			boolean[] keysDown = new boolean[8];
			int oldDir = -2;
			
			protected boolean dkd( int dir ) {
				return keysDown[dir] || keysDown[dir+4];
			}
			
			final int[] dirs = new int[] {
				5,  6, 7,
				4, -1, 0,
				3,  2, 1
			};
			
			protected int dir( int dirX, int dirY ) {
				return dirs[(dirY+1)*3 + dirX+1];
			}
			
			protected void keySomething( int keyCode, boolean state ) {
				int dkCode;
				switch( keyCode ) {
				case KeyEvent.VK_W: dkCode = 3; break;
				case KeyEvent.VK_A: dkCode = 2; break;
				case KeyEvent.VK_S: dkCode = 1; break;
				case KeyEvent.VK_D: dkCode = 0; break;
				case KeyEvent.VK_UP: dkCode = 7; break;
				case KeyEvent.VK_LEFT: dkCode = 6; break;
				case KeyEvent.VK_DOWN: dkCode = 5; break;
				case KeyEvent.VK_RIGHT: dkCode = 4; break;
				default: return; // Not a key we care about
				}
				
				keysDown[dkCode] = state;
				int dirX, dirY;
				if( dkd(0) && !dkd(2) ) {
					dirX = 1;
				} else if( dkd(2) && !dkd(0) ) {
					dirX = -1;
				} else {
					dirX = 0;
				}
				if( dkd(1) && !dkd(3) ) {
					dirY = 1;
				} else if( dkd(3) && !dkd(1) ) {
					dirY = -1;
				} else {
					dirY = 0;
				}
				
				int dir = dir(dirX, dirY);
				if( dir != oldDir ) {
					messageQueue.add(Message.create(playerId, MessageType.INCOMING_PACKET, Integer.valueOf(dir)));
					oldDir = dir;
				}
			}
			
			@Override public void keyPressed(KeyEvent kevt) {
				keySomething(kevt.getKeyCode(), true);
			}

			@Override public void keyReleased(KeyEvent kevt) {
				keySomething(kevt.getKeyCode(), false);
			}

			@Override public void keyTyped(KeyEvent arg0) {}
		});
		c.messageQueue = messageQueue;
		
		final JetManIcons jetManIcons = JetManIcons.load(resourceContext);
		
		final Simulator sim;
		{
			NonTile playerNonTile = JetManBehavior.createJetMan(playerId, clientBa, jetManIcons);
			World world = DemoWorld.initWorld(resourceContext).withNonTile(playerNonTile);
			sim = new Simulator( world, 50 );
		}
		
		// Maybe the simulator should do this
		Thread simulatorThread = new Thread("Incoming message enqueuer") {
			@Override public void run() {
				try {
					_run();
				} catch( Exception e ) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			
			public void _run() throws Exception {
				sim.start();
				
				while( true ) {
					sim.enqueueMessage(messageQueue.take());
				}
			}
		};
		simulatorThread.setDaemon(true);
		simulatorThread.start();
		
		Thread clientUpdateThread = new Thread("Client updater") {
			public void run() {
				while(true) {
					Message m;
					try {
						m = sim.outgoingMessages.take();
					} catch( InterruptedException e ) {
						System.err.println(getName()+" interrupted; quitting");
						e.printStackTrace();
						return;
					}
					
					c.updateReceived();
					if( m.payload instanceof Scene ) {
						c.setScene((Scene)m.payload);
					} else if( m.payload instanceof JetManCoreStats ) {
						c.setStats((JetManCoreStats)m.payload);
					} else {
						System.err.println("Unrecognized message payload: "+m.payload.getClass());
					}
				}
			}
		};
		clientUpdateThread.setDaemon(true);
		clientUpdateThread.start();
	}
}
