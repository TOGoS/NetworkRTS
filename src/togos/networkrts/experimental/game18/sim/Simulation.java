package togos.networkrts.experimental.game18.sim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import togos.networkrts.experimental.game18.sim.Message.MessageType;

public class Simulation
{
	public SimNode root = new SimOrgNode(new SimNode[0]);
	protected final ArrayList<Message> messageQueue = new ArrayList<Message>();
	
	public long getNextAutoUpdateTime() {
		return root.getNextAutoUpdateTime();
	}
	
	public void update( long timestamp, Message m ) {
		messageQueue.clear();
		root = root.update( root, timestamp, m, messageQueue );
		for( int i=0; i<messageQueue.size(); ++i ) {
			// Note that messageQueue may get longer while we're traversing it, and that's okay and expected!
			root = root.update( root, timestamp, messageQueue.get(i), messageQueue );
		}
	}
	
	static class Thumper extends SimpleSimNode {
		final long initialThump;
		final long targetId;
		final int thumpInterval;
		
		public Thumper( long id, long targetId, long initialThump, int interval ) {
			super(id);
			this.targetId = targetId;
			this.initialThump = initialThump;
			this.thumpInterval = interval;
		}
		
		@Override public long getNextAutoUpdateTime() {
			return initialThump;
		}
		
		@Override public Thumper update( SimNode rootNode, long timestamp, Message m, List<Message> messageDest ) {
			long nextThump = initialThump;
			while( timestamp >= nextThump ) {
				messageDest.add(new Message(targetId, targetId, Message.MessageType.INFORMATIONAL, "Thump at "+initialThump));
				nextThump += thumpInterval;
			}
			
			if( !Util.rangeContains(m.minId, m.maxId, id) ) m = Message.NONE;
			
			switch( m.type ) {
			case INFORMATIONAL:
				return new Thumper(id, targetId, nextThump, ((Number)m.payload).intValue() );
			case DELETE:
				return null;
			default:
				return nextThump == initialThump ? this : new Thumper(id, targetId, nextThump, thumpInterval); 
			}
		}
	}
	
	// A silly demonstration
	public static void main( String[] args ) throws InterruptedException {
		Simulation sim = new Simulation();
		
		final long thumperId = 1;
		final long loggerId = 2;
		
		sim.root = new SimOrgNode(new SimNode[] {
			new Logger(loggerId, System.err),
			new Thumper(thumperId, loggerId, 100, 1)
		});
		
		final BlockingQueue<Message> imq = new ArrayBlockingQueue<Message>(1);
		new Thread("Input reader") {
			@Override public void run() {
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					String line;
					while( (line = br.readLine()) != null ) {
						if( line.startsWith("interval ") ) {
							imq.put(new Message(thumperId, MessageType.INFORMATIONAL, Integer.valueOf(line.substring(9))));
						} else {
							imq.put(new Message(loggerId, MessageType.INFORMATIONAL, "User said: "+line));
						}
					}
				} catch( IOException e ) {
					e.printStackTrace();
				} catch( InterruptedException e ) {
					e.printStackTrace();
					Thread.currentThread().interrupt();
				}
			}
		}.start();
		new InteractiveSimulationRunner(imq, sim).run();
		System.exit(0);
	}
}
