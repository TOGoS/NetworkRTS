package togos.networkrts.experimental.game19.util;

import java.util.HashSet;

import togos.networkrts.experimental.game19.sim.MessageSender;
import togos.networkrts.experimental.game19.world.Message;

/**
 * Delivers a message to all handlers, including the one that sent it,
 * if it is registered as a handler.  It is the responsibility
 * of the handlers to check that the message is intended for them.
 */
public class MessageBus implements MessageSender
{
	protected final HashSet<MessageSender> handlers = new HashSet<MessageSender>();
	
	public void addHandler( MessageSender s ) {
		handlers.add(s);
	}
	
	@Override public void sendMessage(Message m) {
		for( MessageSender handler : handlers ) handler.sendMessage(m);
	}
}
