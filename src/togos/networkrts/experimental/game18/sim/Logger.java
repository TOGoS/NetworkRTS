package togos.networkrts.experimental.game18.sim;

import java.io.PrintStream;
import java.util.List;

import togos.networkrts.experimental.game18.sim.Message.MessageType;

public class Logger extends SimpleSimNode
{
	final PrintStream stream;
	
	public Logger( long id, PrintStream stream ) {
		super(id);
		this.stream = stream;
	}
	
	@Override public SimNode update( SimNode rootNode, long timestamp, Message m, List<Message> messageDest ) {
		if( m.type != MessageType.NOOP ) {
			stream.println(m.type+": "+m.payload);
		}	
		return this;
	}
}
