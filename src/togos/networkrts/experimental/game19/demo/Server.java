package togos.networkrts.experimental.game19.demo;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.sim.Simulator;
import togos.networkrts.experimental.game19.world.Message;

public class Server
{
	protected final ResourceContext resourceContext = new ResourceContext(new File(".ccouch"));
	protected Simulator simulator;
	protected final Thread incomingMessagePoster = new Thread("Incoming Message Poster") {
		@Override public void run() {
			try {
				_run();
			} catch( Exception e ) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		public void _run() throws Exception {
			while( true ) {
				simulator.enqueueMessage(incomingMessageQueue.take());
			}
		}
	};
	
	public final LinkedBlockingQueue<Message> incomingMessageQueue = new LinkedBlockingQueue<Message>();
	// TODO: It may be that this shouldn't be directly exposed
	public LinkedBlockingQueue<Message> outgoingMessageQueue;
	
	public Server() throws IOException {
	}
	
	public void init(Simulator sim) throws IOException {
		this.simulator = sim;
		this.outgoingMessageQueue = simulator.outgoingMessages;
	}
	
	protected void ensureInitialized() {
		if( simulator == null ) throw new RuntimeException("Server#simulator is null!");
	}
	
	public void start() {
		ensureInitialized();
		simulator.start();
		incomingMessagePoster.start();
	}
	
	public void setDaemon(boolean d) {
		ensureInitialized();
		simulator.setDaemon(d);
		incomingMessagePoster.setDaemon(d);
	}
}
