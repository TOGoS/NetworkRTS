package togos.networkrts.experimental.game19.sim;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

import togos.networkrts.experimental.game19.io.CerealWorldIO;
import togos.networkrts.experimental.game19.world.ArrayMessageSet;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.Messages;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.EntityAggregation;
import togos.networkrts.experimental.gameengine1.index.EntityRanges;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.experimental.gameengine1.index.EntityUpdater;
import togos.networkrts.experimental.gensim.AutoEventUpdatable2;
import togos.networkrts.experimental.packet19.CoAPMessage;
import togos.networkrts.experimental.packet19.EthernetFrame;
import togos.networkrts.experimental.packet19.IP6Address;
import togos.networkrts.experimental.packet19.IPPacket;
import togos.networkrts.experimental.packet19.MalformedDataException;
import togos.networkrts.experimental.packet19.PacketPayloadCodec;
import togos.networkrts.experimental.packet19.RESTMessage;
import togos.networkrts.experimental.packet19.RESTRequest;
import togos.networkrts.experimental.packet19.UDPPacket;

/**
 * The pure-ish, non-threaded part of the simulator
 */
public class Simulation implements AutoEventUpdatable2<Message>
{
	static class NNTLNonTileUpdateContext implements NonTileUpdateContext {
		protected final Collection<NonTile> nonTileList;
		protected final UpdateContext updateContext;
		
		private NNTLNonTileUpdateContext( UpdateContext updateContext, Collection<NonTile> nonTileList ) {
			this.updateContext = updateContext;
			this.nonTileList = nonTileList;
		}
		
		public static NNTLNonTileUpdateContext get( NNTLNonTileUpdateContext oldInstance, UpdateContext updateContext, Collection<NonTile> nonTileList ) {
			return oldInstance == null || oldInstance.nonTileList != nonTileList ?
				new NNTLNonTileUpdateContext(updateContext, nonTileList) : oldInstance;
		}
		
		@Override public void sendMessage( Message m ) { updateContext.sendMessage(m); }
		@Override public void startAsyncTask( AsyncTask at ) { updateContext.startAsyncTask(at); }
		@Override public void addNonTile( NonTile nt ) { nonTileList.add(nt); }
	}
	
	protected World world;
	protected CerealWorldIO cerealWorldIo;
	protected long simulationBitAddress;
	protected long simulationEthernetAddress;
	protected IP6Address simulationIpAddress;
	protected long time = 0;
	/** Tasks to be done later will be sent here! */
	protected final LinkedBlockingQueue<AsyncTask> asyncTaskQueue;
	/** Messages to things outside the simulation go here! */
	protected final LinkedBlockingQueue<Message> outgoingMessageQueue;
	
	class SimUpdateContext implements UpdateContext {
		public final ArrayMessageSet newMessages = new ArrayMessageSet();
		
		@Override public void sendMessage( Message m ) {
			long targetType = m.maxBitAddress & BitAddresses.TYPE_MASK;
			if( targetType == BitAddresses.TYPE_EXTERNAL ) {
				outgoingMessageQueue.add(m);
			} else {
				newMessages.add(m);
			}
		}
		@Override public void startAsyncTask( AsyncTask at ) {
			asyncTaskQueue.add(at);
		}
	}
	
	public Simulation(World world, LinkedBlockingQueue<AsyncTask> asyncTaskQueue, LinkedBlockingQueue<Message> outgoingMessageQueue ) {
		this.world = world;
		this.asyncTaskQueue = asyncTaskQueue;
		this.outgoingMessageQueue = outgoingMessageQueue;
	}
	
	public World getWorld() {
		return world;
	}
	
	protected boolean needsUpdate( long time, int phase, EntityAggregation er, MessageSet messages ) {
		return
			(phase == 2 && Messages.isApplicableTo(messages, er)) ||
			(er.getNextAutoUpdateTime() <= time && BitAddresses.containsFlag(er.getMaxBitAddress(), BitAddresses.phaseUpdateFlag(phase)));
	}
	
	protected EntitySpatialTreeIndex<NonTile> updateNonTiles( final World world, final long time, final MessageSet incomingMessages, final UpdateContext updateContext, final int phase ) {
		return world.nonTiles.updateEntities(EntityRanges.BOUNDLESS, new EntityUpdater<NonTile>() {
			// TODO: this is very unoptimized
			// it doesn't take advantage of the tree structure at all
			// for determining what messages need to be delivered, etc
			// which could be a problem if there are lots of entities * lots of messages
			
			NNTLNonTileUpdateContext nntlntuc;
			
			@Override public NonTile update(NonTile nt, Collection<NonTile> generatedNonTiles) {
				return nt.update( time, phase, world, incomingMessages,
					NNTLNonTileUpdateContext.get(nntlntuc, updateContext, generatedNonTiles));
			}
		});
	}
	
	static class PacketWrapping {
		public final PacketWrapping parent;
		public final Object payload;
		
		public PacketWrapping( PacketWrapping parent, Object payload ) {
			this.parent = parent;
			this.payload = payload;
		}
		
		public PacketWrapping( Object payload ) {
			this( null, payload );
		}
	}
	
	protected <T> PacketWrapping wrapToAppLayer( PacketWrapping pw, Class<T> appClass, PacketPayloadCodec<T> appCodec ) throws MalformedDataException {
		if( pw.payload instanceof Message ) {
			pw = new PacketWrapping(pw, ((Message)pw.payload).payload );
		}
		if( pw.payload instanceof EthernetFrame ) {
			EthernetFrame f = (EthernetFrame)pw.payload;
			if( f.getDestinationAddress() != simulationEthernetAddress ) {
				System.err.println("Got a misaddressed ethernet packet");
				return null;
			}
			pw = new PacketWrapping(pw, f.getPayload().getPayload(IPPacket.class, IPPacket.CODEC));
		}
		if( pw.payload instanceof IPPacket ) {
			IPPacket ip = (IPPacket)pw.parent;
			if( !ip.getDestinationAddress().equals(simulationIpAddress) ) {
				System.err.println("Got a misaddressed IP packet");
				return null;
			}
			pw = new PacketWrapping(pw, ip.getPayload());
		}
		if( pw.payload instanceof UDPPacket ) {
			pw = new PacketWrapping(pw, ((UDPPacket)pw.payload).getPayload().getPayload(appClass, appCodec));
			// Assume a CoAP message?
		}
		return pw;
	}
	
	protected World handleSimMessage(World world, PacketWrapping pw, SimUpdateContext updateContext) throws MalformedDataException {
		pw = wrapToAppLayer(pw, CoAPMessage.class, CoAPMessage.CODEC);
		if( pw == null ) return world;
		handleRestRequest: if( pw.payload instanceof RESTMessage ) {
			RESTMessage rm = (RESTMessage)pw.payload;
			if( rm.getRestMessageType() != RESTMessage.RESTMessageType.REQUEST ) break handleRestRequest; 
			
			if( RESTRequest.PUT.equals(rm.getMethod()) && "/world".equals(rm.getPath()) ) {
				world = (World)rm.getPayload().getPayload(Object.class, cerealWorldIo);
				System.err.println("World replaced with "+world);
			}
		}
		return world;
	}
	
	protected World processSimMessage(World world, Message m, SimUpdateContext updateContext) {
		if( m.type == MessageType.INCOMING_PACKET ) {
			try {
				world = handleSimMessage(world, new PacketWrapping(m), updateContext);
			} catch( MalformedDataException e ) {
				System.err.println("Incoming message was malformed: "+e.getMessage());
			}
		} else {
			System.err.println("Simulation doesn't know how to handle message type "+m.type);
		}
		return world;
	}
	
	protected World update( long time, int phase, World world, MessageSet incomingMessages, SimUpdateContext updateContext ) {
		// Update RST when phase = 2
		RSTNode rst;
		if( phase == 2 ) {
			for( Message m : Messages.subsetApplicableTo(incomingMessages, simulationBitAddress) ) {
				world = processSimMessage(world, m, updateContext);
			}
			
			int rstSize = 1<<world.rstSizePower;
			rst = world.rst.update( -rstSize/2, -rstSize/2, world.rstSizePower, time, incomingMessages, updateContext );
		} else {
			rst = world.rst;
		}
		
		return new World(
			rst, world.rstSizePower,
			updateNonTiles(world, time, incomingMessages, updateContext, phase),
			world.background
		);
	}
	
	protected void update( long time, MessageSet incomingMessages ) {
		assert time < Long.MAX_VALUE;
		
		if( needsUpdate(time, 1, world, MessageSet.EMPTY) ) {
			SimUpdateContext updateContext = new SimUpdateContext();
			world = update(time, 1, world, MessageSet.EMPTY, updateContext);
			incomingMessages = Messages.union(incomingMessages, updateContext.newMessages);
		}
		while( needsUpdate(time, 2, world, incomingMessages) ) {
			SimUpdateContext updateContext = new SimUpdateContext();
			world = update(time, 2, world, incomingMessages, updateContext);
			incomingMessages = updateContext.newMessages;
			this.time = time;
		}
	}
	
	public Simulation update( long time, Collection<Message> events ) {
		update( time, ArrayMessageSet.getMessageSet(events) );
		return this;
	}
	
	@Override public long getNextAutoUpdateTime() {
		return world.getNextAutoUpdateTime();
	}
	
	@Override public long getCurrentTime() { return time; }
}
