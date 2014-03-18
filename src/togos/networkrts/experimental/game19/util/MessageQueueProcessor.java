package togos.networkrts.experimental.game19.util;

import java.util.concurrent.BlockingQueue;

import togos.networkrts.experimental.game19.sim.MessageSender;
import togos.networkrts.experimental.game19.world.Message;

/**
 * Reads messages from a queue and forwards them to a handler.
 */
public class MessageQueueProcessor extends Thread {
	protected BlockingQueue<Message> queue;
	protected MessageSender handler;
	
	public MessageQueueProcessor( BlockingQueue<Message> queue, MessageSender sender ) {
		super("Outgoing message sender");
		this.queue = queue;
	}
		
	public void run() {
		while(!interrupted()) {
			Message m;
			try {
				m = queue.take();
			} catch( InterruptedException e ) {
				return;
			}
			handler.sendMessage(m);
		}
	}
	
	public void halt() {
		interrupt();
	}
}
