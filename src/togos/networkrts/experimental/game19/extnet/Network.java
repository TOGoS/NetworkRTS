package togos.networkrts.experimental.game19.extnet;

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import togos.networkrts.experimental.game19.util.MessageSender;
import togos.networkrts.experimental.game19.world.Message;

/**
 * A collection of network components.
 * Also provides synchronous message delivery via sendMessage
 * (which should be thread-safe),
 * and asynchronous via incomingMessageQueue.
 *
 * What's the point of the network layer?
 * Why not just have components implement Pushable<EthernetFrame> and link them directly together?
 * 
 * - Messages have a return address;
 *   otherwise return pushers would have to be pushed around
 * - Components can be removed from the network
 *   without having to be unhooked from all their
 *   connections
 * - Components can be addressed e.g. for interactive debugging purposes
 * - Network provides a central place to plug things into
 *   and start/stop everything in the system.
 * 
 * On the other hand, the network *does* add a layer of complexity
 * and run-time cost.  For the moment I feel it's worth it.
 */
public class Network implements NetworkComponent
{
	static class Deliverator extends Thread {
		protected final BlockingQueue<Message> messageQueue;
		protected final MessageSender sender;
		
		public Deliverator( BlockingQueue<Message> q, MessageSender s ) {
			this.messageQueue = q;
			this.sender = s;
		}
		
		@Override public void run() {
			try {
				while( true ) sender.sendMessage(messageQueue.take());
			} catch( InterruptedException e ) {
				// This is the normal way for the thing to stop
			}
		}
	}
	
	//protected final Simulator simulator;
	
	protected final HashSet<NetworkComponent> components = new HashSet<NetworkComponent>();
	public final LinkedBlockingQueue<Message> incomingMessageQueue = new LinkedBlockingQueue<Message>();
	protected final Deliverator deliverator = new Deliverator(incomingMessageQueue, this);
	
	boolean started = false, stopped = false;
	
	public synchronized void addComponent( NetworkComponent nc ) {
		components.add(nc);
	}
	
	public void sendMessage( Message m ) {
		for( NetworkComponent c : components ) c.sendMessage(m);
	}
	
	public Network() { }
	
	public synchronized void start() {
		assert !started;
		started = true;
		for( NetworkComponent c : components ) c.start();
		deliverator.start();
	}
	
	public void setDaemon(boolean d) {
		for( NetworkComponent c : components ) c.setDaemon(d);
		deliverator.setDaemon(d);
	}
	
	public synchronized void halt() {
		assert started;
		stopped = true;
		for( NetworkComponent c : components ) c.halt();
		deliverator.interrupt();
	}
}
