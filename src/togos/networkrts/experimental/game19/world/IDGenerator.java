package togos.networkrts.experimental.game19.world;

public class IDGenerator {
	int next;
	
	public IDGenerator( int startAt ) {
		next = startAt;
	}
	
	public IDGenerator() { this(1); }
	
	public synchronized int newId() {
		return next++;
	}
}
