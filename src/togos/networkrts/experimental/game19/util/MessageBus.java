package togos.networkrts.experimental.game19.util;

import java.util.HashSet;

import togos.networkrts.experimental.game19.sim.MessageSender;
import togos.networkrts.experimental.game19.world.Message;

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
