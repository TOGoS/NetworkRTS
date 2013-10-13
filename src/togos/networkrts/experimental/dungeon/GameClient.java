package togos.networkrts.experimental.dungeon;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import togos.networkrts.experimental.dungeon.DungeonGame.MessageReceiver;
import togos.networkrts.experimental.dungeon.DungeonGame.Simulator;
import togos.networkrts.experimental.dungeon.DungeonGame.UpdateListener;
import togos.networkrts.experimental.dungeon.DungeonGame.VisibilityCache;
import togos.networkrts.experimental.dungeon.DungeonGame.WalkCommand;
import togos.networkrts.experimental.dungeon.net.ObjectEthernetFrame;
import togos.networkrts.experimental.gensim.EventLoop;
import togos.networkrts.experimental.gensim.QueuelessRealTimeEventSource;
import togos.networkrts.util.LossyQueue;

public class GameClient implements MessageReceiver<ObjectEthernetFrame<?>>
{
	/**
	 * A thread that halts the entire process when any exception is thrown.
	 */
	static abstract class CoreThread extends Thread {
		public CoreThread( String name ) {
			super(name);
		}
		
		protected abstract void _run() throws Throwable;
		
		public final void run() {
			try {
				_run();
			} catch( Throwable t ) {
				System.err.println("Core thread "+getName()+" crashed");
				t.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	static class ImageCanvas extends Canvas
	{
		private static final long serialVersionUID = 1L;
		
		public ImageCanvas() {
			setImage(null);
		}
		
		BufferedImage image;
		public void setImage( BufferedImage img ) {
			this.image = img;
			setPreferredSize( img == null ? new Dimension(512,384) : new Dimension(img.getWidth(), img.getHeight()));
			repaint();
		}
		
		protected int offX, offY;
		protected float scale;
		
		/**
		 * Adjust scale and offsets and return the image so
		 * that this.image can be safely rebound from another thread
		 */
		protected BufferedImage initOffsets() {
			BufferedImage img = image;
			
			if( img == null ) return img;
			
			scale = 1;
			while( img.getWidth() * scale > getWidth() || img.getHeight() * scale > getHeight() ) {
				scale /= 2;
			}
			while( img.getWidth() * scale * 2 <= getWidth() && img.getHeight() * 2 <= getHeight() ) {
				scale *= 2;
			}
			
			offX = (int)(getWidth() - img.getWidth() * scale) / 2;
			offY = (int)(getHeight() - img.getHeight() * scale) / 2;
			
			return img;
		}
		
		@Override public void paint( Graphics g ) {
			g.setColor(getBackground());
			BufferedImage img = initOffsets();
			if( img == null || scale == 0 ) {
				g.fillRect(0,0,getWidth(),getHeight());
			} else {
				int scaledImageWidth  = (int)(img.getWidth()  * scale);
				int scaledImageHeight = (int)(img.getHeight() * scale);
				int right  = offX + scaledImageWidth; 
				int bottom = offY + scaledImageHeight;
				synchronized( img ) {
					g.drawImage( img, offX, offY, scaledImageWidth, scaledImageHeight, null );
				}
				g.fillRect(    0, offY,             offX, scaledImageHeight);
				g.fillRect(right, offY, getWidth()-right, scaledImageHeight);
				g.fillRect(0,      0, getWidth(), offY);
				g.fillRect(0, bottom, getWidth(), getHeight()-bottom);
			}
		}
		
		@Override public void update( Graphics g ) { paint(g); }
	}
	
	// Threads:
	//   run simulation
	//   encode and send player's projection
	// --- [fake] network layer ---
	//   decode player's projection
	//   draw projection to buffer
	//   AWT thread repaints as needed
	
	public long playerEthernetAddress;
	public long clientEthernetAddress;
	protected final BlockingQueue<ObjectEthernetFrame<?>> commandQueue = new LinkedBlockingQueue<ObjectEthernetFrame<?>>();
	protected final BlockingQueue<Projection> projectionQueue = new LossyQueue<Projection>();	
	
	/**
	 * Packet incoming from simulator (or somewhere)
	 */
	@Override public void messageReceived(ObjectEthernetFrame<?> f) {
		if( f.payload instanceof Projection ) {
			projectionQueue.add( (Projection)f.payload );
		}
	}
	
	public void openUi() {
		BufferedImage img = new BufferedImage( 768, 512, BufferedImage.TYPE_INT_ARGB );
		Graphics g = img.getGraphics();
		g.setColor(Color.DARK_GRAY);
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.setColor(Color.RED);
		g.drawString("Initializing...", 4, 12);
		
		final ImageCanvas leCanv = new ImageCanvas();
		leCanv.setImage(img);
		leCanv.setBackground(new Color(0.1f, 0, 0));
		leCanv.addKeyListener( new KeyListener() {
			boolean walkLeft  = false;
			boolean walkRight = false;
			boolean walkUp    = false;
			boolean walkDown  = false;
			
			protected void updateWalking() {
				WalkCommand cmd = new WalkCommand();
				cmd.walkX = (walkLeft && !walkRight) ? -1 : (walkRight && !walkLeft) ? 1 : 0;
				cmd.walkY = (walkUp   && !walkDown ) ? -1 : (walkDown  && !walkUp  ) ? 1 : 0;
				try {
					commandQueue.put(new ObjectEthernetFrame<WalkCommand>(0, playerEthernetAddress, cmd));
				} catch( InterruptedException e ) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
			}
			
			@Override public void keyPressed( KeyEvent kevt ) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): walkUp    = true; break;
				case( KeyEvent.VK_DOWN  ): walkDown  = true; break;
				case( KeyEvent.VK_LEFT  ): walkLeft  = true; break;
				case( KeyEvent.VK_RIGHT ): walkRight = true; break;
				default:
					System.err.println(kevt.getKeyCode());
				}
				updateWalking();
			}
			
			@Override public void keyReleased( KeyEvent kevt ) {
				switch( kevt.getKeyCode() ) {
				case( KeyEvent.VK_UP    ): walkUp    = false; break;
				case( KeyEvent.VK_DOWN  ): walkDown  = false; break;
				case( KeyEvent.VK_LEFT  ): walkLeft  = false; break;
				case( KeyEvent.VK_RIGHT ): walkRight = false; break;
				}
				updateWalking();
			}
			
			@Override public void keyTyped( KeyEvent kevt ) {
			}
		});
		
		final Frame win = new Frame("DungeonGame client thingy");
		win.add( leCanv );
		win.pack();
		win.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing( WindowEvent evt ) {
				System.exit(0);
			}
		});
		
		win.setVisible(true);
		leCanv.requestFocus();
		
		new CoreThread("Renderer") {
			BlockFieldRenderer renderer = new BlockFieldRenderer();
			BufferedImage b = null;
			
			protected void handleProjection( Projection p ) {
				int dWidth = 640;
				int dHeight = 480;
				
				if( b == null || b.getWidth() != dWidth || b.getHeight() != dHeight ) {
					b = new BufferedImage(dWidth, dHeight, BufferedImage.TYPE_INT_ARGB);
				}
				synchronized( p.blockField ) {
					synchronized( b ) {
						Graphics g = b.getGraphics();
						g.setColor(Color.BLACK);
						g.fillRect(0, 0, dWidth, dHeight);
						renderer.render(p.blockField, p.originX, p.originY, g, 0, 0, dWidth, dHeight);
					}
				}
				leCanv.setImage(b);
			}
			
			public void _run() throws InterruptedException {
				while( true ) {
					handleProjection( projectionQueue.take() ); 
				}
			};
		}.start();
	}
	
	public static void main( String[] args ) {
		final long playerEthernetAddress = 0x1234567;
		final long clientEthernetAddress = 0x1234566;
		
		final GameClient client = new GameClient();
		client.playerEthernetAddress = playerEthernetAddress;
		client.clientEthernetAddress = clientEthernetAddress;
		
		//// Server stuff ////
		
		final QueuelessRealTimeEventSource<ObjectEthernetFrame<?>> evtReg = new QueuelessRealTimeEventSource<ObjectEthernetFrame<?>>();
		final Simulator sim = DungeonGame.initSim(evtReg.getCurrentTime());
		
		final VisibilityCache playerVc = new VisibilityCache(32, 32, 8, sim.getInternalUpdater());
		playerVc.addUpdateListener(new UpdateListener() {
			@Override public void updated() {
				sim.commandee.uplink.messageReceived(new ObjectEthernetFrame<Projection>(0, 0, playerVc.projection.clone()));
			}
		});
		
		sim.commandee.uplinkInterfaceAddress = playerEthernetAddress;
		sim.commandee.setVisibilityCache(playerVc); 
		sim.commandee.clientEthernetAddress = clientEthernetAddress;
		sim.commandee.uplink = client;
		/*
		sim.commandee.projectionTransmitter = new Transmitter<Projection>() {
			@Override public void send(Projection projection) {
				try {
					gameClient.put( sim.getCurrentTime(), new ObjectEthernetFrame(playerEthernetAddress, clientEthernetAddress, projection.clone()));
				} catch( InterruptedException e ) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		};
		*/
		
		new CoreThread("Simulation runner") {
			public void _run() throws Exception {
				EventLoop.run(evtReg, sim);
			};
		}.start();
		
		new CoreThread("Command reader") {
			public void _run() throws InterruptedException {
				while(true) {
					evtReg.post(client.commandQueue.take());
				}
			}
		}.start();
		
		client.openUi();
	}
}
