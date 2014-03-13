package togos.networkrts.experimental.game19.demo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.sim.MessageSender;
import togos.networkrts.experimental.game19.sim.Simulator;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStackRSTNode;
import togos.networkrts.experimental.game19.world.IDGenerator;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.QuadRSTNode;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.RSTUtil;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.beh.NoBehavior;
import togos.networkrts.experimental.game19.world.gen.SolidNodeFiller;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManBehavior;
import togos.networkrts.experimental.game19.world.thing.jetman.JetManIcons;
import togos.networkrts.experimental.gameengine1.index.AABB;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.experimental.packet19.EthernetFrame;
import togos.networkrts.experimental.shape.TCircle;
import togos.networkrts.util.BitAddressUtil;

public class Server
{
	final ResourceContext resourceContext = new ResourceContext(new File(".ccouch"));
	final IDGenerator idGenerator = new IDGenerator();
	
	final JetManIcons jetManIcons = JetManIcons.load(resourceContext);
	
	public Server() throws IOException {
	}
	
	protected NonTile makePlayerJetMan( ) {
		int playerId = idGenerator.newId();
		long playerBa = BitAddresses.forceType(BitAddresses.TYPE_NONTILE, playerId);
		return new NonTile(0, 0, 0, 0, 0,
			new AABB(-0.25, -0.5, -0.5, +0.25, +0.5, +0.5),
			playerBa, playerBa, 1,
			jetManIcons.walking[0], 
			new JetManBehavior(playerBa, BitAddressUtil.NO_ADDRESS, jetManIcons)
		);
	}
	
	public World initWorld() throws IOException {
		ImageHandle brickImage = resourceContext.storeImageHandle(new File("tile-images/dumbrick1.png"));
		//ImageHandle dudeImage = resourceContext.storeImageHandle(new File("tile-images/dude.png"));
		//ImageHandle ballImage = resourceContext.storeImageHandle(new File("tile-images/stupid-ball.png"));
		
		Block bricks = new Block(BitAddresses.BLOCK_SOLID|BitAddresses.BLOCK_OPAQUE, brickImage, NoBehavior.instance);
		
		World world;
		int worldSizePower = 24;
		int worldDataOrigin = -(1<<(worldSizePower-1));
		
		RSTNode n = QuadRSTNode.createHomogeneous(bricks.stack, worldSizePower);
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( -2, -2, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( +2, +2, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		
		Random r = new Random();
		for( int i=0; i<100; ++i ) {
			n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( r.nextGaussian()*20, r.nextGaussian()*20, r.nextDouble()*8 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
		}
			
		EntitySpatialTreeIndex<NonTile> nonTiles = new EntitySpatialTreeIndex<NonTile>();
		return new World(n, worldSizePower, nonTiles);
	}
	
	class Switch implements MessageSender {
		@Override public void sendMessage(Message m) {
			Object p = m.payload;
			if( p instanceof EthernetFrame ) {
				// All is well!  Let's switch it!
				// Will need to:
				//   Map the destination MAC address to either
				//   - a bit address (indicating the message goes into the simulation), or
				//   - something indicating to actually send the packet out across the real internet
			} else {
				System.err.println("External switch received something other than an ethernet frame :X");
			}
		}
	}
	
	class OutgoingMessageSender extends Thread {
		protected BlockingQueue<Message> outgoingMessages;
		
		final Map<Long,MessageSender> externalMessageHandlers = new HashMap<Long,MessageSender>();
		
		public OutgoingMessageSender( BlockingQueue<Message> outgoingMessages ) {
			super("Outgoing message sender");
			this.outgoingMessages = outgoingMessages;
		}
		
		protected void handle( Message m ) {
		}
		
		public void run() {
			while(true) {
				Message m;
				try {
					m = outgoingMessages.take();
				} catch( InterruptedException e ) {
					System.err.println(getName()+" interrupted; quitting");
					e.printStackTrace();
					return;
				}
			}
		}
	}
	
	Simulator sim;
	public void init() throws IOException {
		sim = new Simulator(initWorld(), 50);
	}
	
	public void start() {
		sim.start();
		// TODO: start listening for packets or whatever, idk
	}
}
