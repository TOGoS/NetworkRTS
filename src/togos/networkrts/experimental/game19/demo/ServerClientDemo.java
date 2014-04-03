package togos.networkrts.experimental.game19.demo;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;

import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.scene.Scene;
import togos.networkrts.experimental.game19.sim.Simulator;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.IDGenerator;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManCoreStats;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManIcons;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManInternals;
import togos.networkrts.experimental.packet19.FakeCoAPMessage;
import togos.networkrts.experimental.packet19.RESTRequest;
import togos.networkrts.experimental.packet19.WackPacket;
import togos.networkrts.util.Predicate;

public class ServerClientDemo
{
	// TODO: Move a lot of the details into Client/Server
	public static void main( String[] args ) throws Exception {
		final Server server = new Server();
		
		IDGenerator idGenerator = new IDGenerator();
		
		final ResourceContext resourceContext = new ResourceContext(new File(".ccouch"));
		final Client c = new Client(resourceContext);
		final long playerId = idGenerator.newId();
		final long playerBa = BitAddresses.forceType(BitAddresses.TYPE_NONTILE, playerId);
		final long clientId = idGenerator.newId();
		final long clientBa = BitAddresses.forceType(BitAddresses.TYPE_EXTERNAL, clientId);
		final long simId = idGenerator.newId();
		final long simBa = BitAddresses.forceType(BitAddresses.TYPE_INTERNAL, simId);
		final World initialWorld;
		{
			final JetManIcons jetManIcons = JetManIcons.load(resourceContext);
			final NonTile playerNonTile = JetManInternals.createJetMan(playerId, clientBa, jetManIcons);
			initialWorld = DemoWorld.initWorld(resourceContext).withNonTile(playerNonTile);
			Simulator sim = new Simulator( initialWorld, 50, simBa );
			sim.setDaemon(true);
			server.init(sim);
		}
		
		c.startUi();
		c.sceneCanvas.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent kevt) {
				switch( kevt.getKeyCode() ) {
				case KeyEvent.VK_EQUALS: c.sceneCanvas.zoomMore(); break;
				case KeyEvent.VK_MINUS: c.sceneCanvas.zoomLess(); break;
				case KeyEvent.VK_C:
					server.incomingMessageQueue.add(Message.create(playerBa, MessageType.INCOMING_PACKET, clientBa, Boolean.TRUE));
					break;
				case KeyEvent.VK_U:
					server.incomingMessageQueue.add(Message.create(playerBa, MessageType.INCOMING_PACKET, clientBa, Boolean.FALSE));
					break;
				case KeyEvent.VK_R:
					// TODO: ethernet frames, etc etc
					FakeCoAPMessage fcm = FakeCoAPMessage.request((byte)0, 0, RESTRequest.PUT, "/world", new WackPacket(initialWorld, Object.class, null));
					server.incomingMessageQueue.add(Message.create(simBa, MessageType.INCOMING_PACKET, clientBa, fcm));
					break;
				}
			}
		});
		c.sceneCanvas.addKeyListener(new KeyListener() {
			boolean[] keysDown = new boolean[8];
			int oldDir = -2; // Unknown!
			
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
				
				// TODO: On Linux, you'll rapidly switch between some key being pressed
				// and not pressed while it's held down.
				// keyReleased will often be followed immediately by keyPressed.
				// Find some way to ignore those.
				
				int dir = dir(dirX, dirY);
				if( dir != oldDir ) {
					Message m = Message.create(playerBa, MessageType.INCOMING_PACKET, clientBa, Integer.valueOf(dir));
					server.incomingMessageQueue.add(m);
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
		c.messageQueue = server.incomingMessageQueue;
		
		// Maybe the simulator should do this

		server.setDaemon(true);
		server.start();
		
		Thread clientUpdateThread = new Thread("Client Updater") {
			public void run() {
				while(true) {
					Message m;
					try {
						m = server.outgoingMessageQueue.take();
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
					} else if( m.payload instanceof String ) {
						c.addTextMessage(new Client.TextMessage(System.currentTimeMillis(), (String)m.payload));
					} else {
						System.err.println("Unrecognized message payload: "+m.payload.getClass());
					}
				}
			}
		};
		clientUpdateThread.setDaemon(true);
		clientUpdateThread.start();
		
		boolean consoleEnabled = true;
		if( consoleEnabled ) {
			BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
			String line;
			while( (line = consoleReader.readLine()) != null ) {
				line = line.trim();
				if( line.length() == 0 || line.startsWith("#") ) {
				} else if( "dump-threads".equals(line) ) {
					dumpThreads( getAllThreads() );
				} else if( "dump-runnable-threads".equals(line) ) {
					dumpThreads( filter(getAllThreads(), new Predicate<Thread>() {
						@Override public boolean test(Thread t) { return t.getState() == Thread.State.RUNNABLE; }
					}));
				} else if( "dump-non-daemon-threads".equals(line) ) {
					dumpThreads( filter(getAllThreads(), new Predicate<Thread>() {
						@Override public boolean test(Thread t) { return !t.isDaemon(); }
					}));
				} else if( "eof".equals(line) ) {
					break;
				} else {
					System.err.println("Unknown command: "+line);
				}
			}
		}
	}
	
	protected static <T> T[] filter( T[] ts, Predicate<T> p ) {
		T[] nooz = Arrays.copyOf(ts,ts.length);
		int j = 0;
		for( int i=0; i<ts.length; ++i ) {
			if( p.test(ts[i]) ) nooz[j++] = ts[i];
		}
		return j < ts.length ? Arrays.copyOf(nooz,j) : nooz;
	}
	
	protected static Thread[] getAllThreads() {
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		while( tg.getParent() != null ) tg = tg.getParent();
		Thread[] threads = new Thread[tg.activeCount()];
		int count;
		while( (count = tg.enumerate( threads, true )) == threads.length ) {
		    threads = new Thread[threads.length * 2];
		}
		return Arrays.copyOf(threads, count);
	}
	
	protected static void dumpThreads( Thread[] threads ) {
		for( Thread t : threads ) {
			Thread.State s = t.getState();
			System.err.println("  "+t.getName()+": "+s+" "+(t.isAlive()?" alive":"")+(t.isDaemon()?" daemon" : ""));
		}
	}
}
