package togos.networkrts.experimental.game19.demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import togos.networkrts.experimental.game19.Renderer;
import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.scene.Scene;
import togos.networkrts.experimental.game19.scene.Layer.VisibilityClip;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManCoreStats;
import togos.networkrts.ui.ImageCanvas;

class Client {
	static class TextMessage implements Comparable<Client.TextMessage> {
		public final long receptionTime;
		public final String text;
		
		public TextMessage( long r, String t ) {
			this.receptionTime = r;
			this.text = t;
		}
		
		@Override public int compareTo(Client.TextMessage o) {
			return receptionTime < o.receptionTime ? -1 : receptionTime > o.receptionTime ? 1 : 0;
		}
	}
	
	public static class UIState {
		public final Scene scene;
		public final JetManCoreStats stats;
		public final List<Client.TextMessage> textMessages;
		public final boolean connected;
		
		public UIState( Scene scene, JetManCoreStats stats, List<Client.TextMessage> textMessages, boolean connected ) {
			assert textMessages != null;
			
			this.scene = scene;
			this.stats = stats;
			this.textMessages = textMessages;
			this.connected = connected;
		}
		
		@Override public boolean equals(Object obj) {
			if( !(obj instanceof Client.UIState) ) return false;
			
			Client.UIState o = (Client.UIState)obj;
			return
				(scene == o.scene || (scene != null && scene.equals(o.scene))) &&
				(stats == o.stats || (stats != null && stats.equals(o.stats))) &&
				textMessages.equals(o.textMessages) && connected == o.connected;
		}
	}
	
	static class SceneCanvas extends ImageCanvas
	{
		private static final long serialVersionUID = 1L;

		protected BufferedImage sceneBuffer; // = new BufferedImage(512, 384, BufferedImage.TYPE_INT_RGB); // Much faster than ARGB!
		protected synchronized BufferedImage getSceneBuffer( int width, int height ) {
			if( sceneBuffer == null || sceneBuffer.getWidth() != width || sceneBuffer.getHeight() != height ) {
				return sceneBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			} else {
				return sceneBuffer;
			}
		}
		
		int pixelsPerMeter = 32;
		
		protected final Renderer renderer;
		public SceneCanvas( ResourceContext resourceContext ) {
			renderer = new Renderer(resourceContext);
			addComponentListener(new ComponentAdapter() {
				@Override public void componentResized(ComponentEvent e) { redrawBuffer(); }
			});
		}
		
		Color sceneBackgroundColor = new Color(0.2f, 0, 0);
		Client.UIState uiState = new UIState(null, null, Collections.<Client.TextMessage>emptyList(), false);
		
		public synchronized void setUiState( Client.UIState s ) {
			if( s.equals(uiState) ) return; 
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
			Client.UIState u = null;
			while( true ) {
				synchronized(this) {
					while( !needRedraw || uiState == null ) wait();
					u = uiState;
					needRedraw = false;
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
				if( wid <= 0 || hei <= 0 ) continue;
				
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
					for( int ti=u.textMessages.size()-1, ty=sb.getHeight()-12; ti>=0; --ti, ty -= 12 ) {
						Client.TextMessage tm = u.textMessages.get(ti);
						float tmOpacity = 1-(System.currentTimeMillis()-tm.receptionTime)/5000f;
						if( tmOpacity <= 0 ) continue;
						g.setColor(new Color(1,1,1,tmOpacity));
						g.drawString( tm.text, 4, ty );
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
	
	Client.SceneCanvas sceneCanvas;
	public Queue<Message> messageQueue;
	protected Scene scene;
	protected JetManCoreStats stats;
	protected List<Client.TextMessage> textMessages = new ArrayList<Client.TextMessage>();
	public long lastUpdateFromAvatar;
	
	public Client( ResourceContext resourceContext ) {
		sceneCanvas = new SceneCanvas(resourceContext);
		sceneCanvas.setBackground(Color.BLACK);
	}
	
	protected void updateUiState() {
		sceneCanvas.setUiState(new UIState(scene, stats, textMessages, lastUpdateFromAvatar >= System.currentTimeMillis() - 1000));
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
	public synchronized void addTextMessage( Client.TextMessage m ) {
		textMessages.add(m);
		Collections.sort(textMessages);
		if( textMessages.size() > 6 ) {
			textMessages = textMessages.subList(textMessages.size()-6, 6);
		}
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
		f.setExtendedState(f.getExtendedState()|Frame.MAXIMIZED_BOTH);
		final Thread watchdogThread = new Thread("UI Watchdog") {
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
		watchdogThread.start();
		final Thread redrawThread = new Thread("Scene Redrawer") {
			@Override public void run() {
				try {
					sceneCanvas.redrawLoop();
				} catch( InterruptedException e ) {
					interrupt();
				}
			}
		};
		redrawThread.start();
		f.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent evt) {
				f.dispose();
				redrawThread.interrupt();
				watchdogThread.interrupt();
			}
		});
		f.pack();
		f.setVisible(true);
	}
}