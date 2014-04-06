package togos.networkrts.experimental.game19.extnet;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import togos.networkrts.experimental.game19.util.MessageSender;
import togos.networkrts.experimental.game19.world.Message;

public class Network implements MessageSender
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
	
	public Network() throws IOException { }
	
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
