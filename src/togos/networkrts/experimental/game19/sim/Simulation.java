package togos.networkrts.experimental.game19.sim;

import java.util.Collection;
import java.util.Queue;

import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.world.ArrayMessageSet;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.Messages;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gameengine1.index.AABB;
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
import togos.networkrts.experimental.packet19.PacketWrapping;
import togos.networkrts.experimental.packet19.RESTMessage;
import togos.networkrts.experimental.packet19.RESTRequest;
import togos.networkrts.experimental.packet19.UDPPacket;
import togos.networkrts.util.BitAddressUtil;

/**
 * The pure-ish, non-threaded part of the simulator
 */
public class Simulation implements AutoEventUpdatable2<Message>, EntityAggregation
{
	public static final double SIMULATED_TICK_INTERVAL = 0.05;
	public static final double REAL_TICK_INTERVAL_TARGET = 0.05;
	public static final double GRAVITY = 9.8;

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
	
	/**
	 * Update context to be used synchronously, within the simulation.
	 */
	private class SimUpdateContext implements UpdateContext {
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
	
	protected World world;
	protected long lastUpdateTick = 0;
	protected long worldTimeOffset = 0;
	protected final ResourceContext resourceContext;
	protected final PacketPayloadCodec<Object> cerealWorldPacketPayloadCodec;
	protected long simulationBitAddress;
	protected long simulationEthernetAddress;
	protected IP6Address simulationIpAddress;
	/** Tasks to be done later will be sent here! */
	protected final Queue<AsyncTask> asyncTaskQueue;
	/** Messages to things outside the simulation go here! */
	protected final Queue<Message> outgoingMessageQueue;
	
	public Simulation(World world, Queue<AsyncTask> asyncTaskQueue, Queue<Message> outgoingMessageQueue, ResourceContext rc) {
		this.setWorld(world);
		this.asyncTaskQueue = asyncTaskQueue;
		this.outgoingMessageQueue = outgoingMessageQueue;
		this.resourceContext = rc;
		this.cerealWorldPacketPayloadCodec = rc.getCerealWorldIo().packetPayloadCodec;
	}
	
	public World getWorld() {
		return world;
	}
	
	public void setWorld( World w ) {
		this.world = w;
		this.worldTimeOffset = world.referenceTime - lastUpdateTick;
		System.err.println("World replaced with "+world);
	}
	
	protected boolean needsUpdate( long time, EntityAggregation er, MessageSet messages ) {
		return Messages.isApplicableTo(messages, er) || er.getNextAutoUpdateTime() <= time;
	}
	
	protected EntitySpatialTreeIndex<NonTile> updateNonTiles( final World world, final long time, final MessageSet incomingMessages, final UpdateContext updateContext) {
		return world.nonTiles.updateEntities(EntityRanges.BOUNDLESS, new EntityUpdater<NonTile>() {
			// TODO: this is very unoptimized
			// it doesn't take advantage of the tree structure at all
			// for determining what messages need to be delivered, etc
			// which could be a problem if there are lots of entities * lots of messages
			
			NNTLNonTileUpdateContext nntlntuc;
			
			@Override public NonTile update(NonTile nt, Collection<NonTile> generatedNonTiles) {
				return nt.update( time, world, incomingMessages,
					NNTLNonTileUpdateContext.get(nntlntuc, updateContext, generatedNonTiles));
			}
		});
	}
	
	protected <T> PacketWrapping<?> wrapToAppLayer( PacketWrapping<?> pw, Class<T> appClass, PacketPayloadCodec<T> appCodec ) throws MalformedDataException {
		if( pw.payload instanceof Message ) {
			pw = new PacketWrapping<Object>(pw, ((Message)pw.payload).payload );
		}
		if( pw.payload instanceof EthernetFrame ) {
			EthernetFrame f = (EthernetFrame)pw.payload;
			if( f.getDestinationAddress() != simulationEthernetAddress ) {
				System.err.println("Got a misaddressed ethernet packet");
				return null;
			}
			if( f.getPayload() instanceof IPPacket ) {
				pw = new PacketWrapping<IPPacket>(pw, (IPPacket)f.getPayload());
			} else {
				System.err.println("Not an IP packet!");
				return null;
			}
		}
		if( pw.payload instanceof IPPacket ) {
			IPPacket ip = (IPPacket)pw.parent;
			if( !ip.getDestinationAddress().equals(simulationIpAddress) ) {
				System.err.println("Got a misaddressed IP packet");
				return null;
			}
			pw = new PacketWrapping<Object>(pw, ip.getPayload());
		}
		if( pw.payload instanceof UDPPacket ) {
			pw = new PacketWrapping<Object>(pw, ((UDPPacket)pw.payload).getPayload().getPayload(appClass, appCodec));
			// Assume a CoAP message?
		}
		return pw;
	}
	
	protected void handleSimMessage(PacketWrapping<?> pw, SimUpdateContext updateContext) throws MalformedDataException {
		pw = wrapToAppLayer(pw, CoAPMessage.class, CoAPMessage.CODEC);
		if( pw == null ) return;
		handleRestRequest: if( pw.payload instanceof RESTMessage ) {
			RESTMessage rm = (RESTMessage)pw.payload;
			if( rm.getRestMessageType() != RESTMessage.RESTMessageType.REQUEST ) break handleRestRequest; 
			
			if( "/world".equals(rm.getPath()) ) {
				if( RESTRequest.PUT.equals(rm.getMethod()) ) {
					setWorld( (World)rm.getPayload().getPayload(Object.class, cerealWorldPacketPayloadCodec) );
				}
			} else if( "/world/saves".equals(rm.getPath()) ) {
				final World w = world;
				if( RESTRequest.POST.equals(rm.getMethod()) ) {
					asyncTaskQueue.add(new AsyncTask() {
						@Override public void run(UpdateContext ctx) {
							System.err.println("Saving world...");
							System.err.println("World = "+resourceContext.getCerealWorldIo().storeObject(w));
						}
					});
				}
			}
		}
	}
	
	protected void processSimMessage(Message m, SimUpdateContext updateContext) {
		if( m.type == MessageType.INCOMING_PACKET ) {
			try {
				handleSimMessage(new PacketWrapping<Message>(m), updateContext);
			} catch( MalformedDataException e ) {
				System.err.println("Incoming message was malformed: "+e.getMessage());
			}
		} else {
			System.err.println("Simulation doesn't know how to handle message type "+m.type);
		}
	}
	
	protected void update( long time, MessageSet incomingMessages, SimUpdateContext updateContext ) {
		this.lastUpdateTick = time;
		
		// Update RST when phase = 2
		RSTNode rst;
		for( Message m : Messages.subsetApplicableTo(incomingMessages, simulationBitAddress) ) {
			processSimMessage(m, updateContext);
		}
		
		int rstSize = 1<<world.rstSizePower;
		rst = world.rst.update( -rstSize/2, -rstSize/2, world.rstSizePower, time, incomingMessages, updateContext );
		
		world = new World(
			time, rst, world.rstSizePower,
			updateNonTiles(world, time + worldTimeOffset, incomingMessages, updateContext),
			world.background
		);
	}
	
	protected void update( long time, MessageSet incomingMessages ) {
		assert time < Long.MAX_VALUE;
		
		while( needsUpdate(time, this, incomingMessages) ) {
			SimUpdateContext updateContext = new SimUpdateContext();
			update(time, incomingMessages, updateContext);
			incomingMessages = updateContext.newMessages;
		}
	}
	
	public Simulation update( long time, Collection<Message> events ) {
		update( time, ArrayMessageSet.getMessageSet(events) );
		return this;
	}
	
	@Override public long getNextAutoUpdateTime() {
		if( world.getNextAutoUpdateTime() <= lastUpdateTick ) return lastUpdateTick+1;
		return world.getNextAutoUpdateTime();
	}
	
	@Override public long getCurrentTime() { return lastUpdateTick; }
	
	@Override public final long getMinBitAddress() {
		return BitAddressUtil.minAddressAI(world.getMinBitAddress(), simulationBitAddress);
	}
	
	@Override public final long getMaxBitAddress() {
		return BitAddressUtil.maxAddressAI(world.getMaxBitAddress(), simulationBitAddress);
	}

	@Override public AABB getAabb() {
		return AABB.BOUNDLESS;
	}
}
