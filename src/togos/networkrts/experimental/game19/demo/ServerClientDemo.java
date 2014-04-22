package togos.networkrts.experimental.game19.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.extnet.Network;
import togos.networkrts.experimental.game19.extnet.NetworkComponent;
import togos.networkrts.experimental.game19.sim.Simulator;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.IDGenerator;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManIcons;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManInternals;
import togos.networkrts.util.BitAddressUtil;
import togos.networkrts.util.Predicate;

public class ServerClientDemo
{
	// TODO: Move a lot of the details into Client/Server
	public static void main( String[] args ) throws Exception {
		final Network server;
		
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
			server = new Network();
			Simulator sim = new Simulator( initialWorld, 50, simBa, server.incomingMessageQueue, resourceContext );
			sim.setDaemon(true);
			server.addComponent(sim);
		}
		
		final LinkedBlockingQueue<Message> clientIncomingMessageQueue = new LinkedBlockingQueue<Message>();
		server.addComponent(new NetworkComponent(){
			@Override public void sendMessage( Message m ) {
				if( BitAddressUtil.rangeContains(m, clientBa)) {
					clientIncomingMessageQueue.add(m);
				}
			}
			
			@Override public void start() {}
			@Override public void setDaemon( boolean d ) {}
			@Override public void halt() {}
		});
		
		c.loadWandBlock(1, new File("things/blocks/gray-stone-bricks0.block"));
		c.loadWandBlock(2, new File("things/blocks/dirt0.block"));
		c.loadWandBlock(3, new File("things/blocks/grass0.block"));
		c.loadWandBlock(4, new File("things/blocks/small-tree0.block"));
		c.loadWandBlock(5, new File("things/blocks/big-gray-spikes0.block"));
		
		c.simulationBitAddress = simBa;
		c.clientBitAddress = clientBa;
		c.playerBitAddress = playerBa;
		c.initialWorld = initialWorld;
		c.outgoingMessageQueue = server.incomingMessageQueue;
		c.incomingMessageQueue = clientIncomingMessageQueue;
		c.startUi();
		
		// Maybe the simulator should do this

		server.setDaemon(true);
		server.start();
		
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
