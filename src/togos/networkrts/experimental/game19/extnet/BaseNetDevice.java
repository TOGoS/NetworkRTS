package togos.networkrts.experimental.game19.extnet;

import togos.networkrts.experimental.game19.sim.AsyncTask;
import togos.networkrts.experimental.game19.sim.UpdateContext;
import togos.networkrts.experimental.game19.util.MessageSender;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.beh.BasePacketHandler;
import togos.networkrts.experimental.packet19.PacketWrapping;
import togos.networkrts.experimental.packet19.UDPPacket;

public class BaseNetDevice implements NetworkComponent
{
	interface UDPHandler {
		public void handleUdp( PacketWrapping<UDPPacket> pw );
	}
	
	protected BasePacketHandler packetHandler;
	protected final MessageSender network;
	
	public boolean debug = false;
	
	protected BaseNetDevice( BasePacketHandler ph, MessageSender network ) {
		this.packetHandler = ph;
		this.network = network;
	}
	
	final UpdateContext updateContext = new UpdateContext() {
		@Override public void sendMessage( Message m ) {
			network.sendMessage(m);
		}

		@Override public void startAsyncTask( AsyncTask at ) {
			throw new UnsupportedOperationException();
		}
	};
	
	@Override public void sendMessage(Message m) {
		packetHandler = packetHandler.update( 0, m, updateContext);
	}
	
	@Override public void start() { }
	@Override public void setDaemon(boolean d) { }
	@Override public void halt() { }
}
