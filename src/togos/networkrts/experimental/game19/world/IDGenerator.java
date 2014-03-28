package togos.networkrts.experimental.game19.world;

public class IDGenerator {
	long next;
	
	public IDGenerator( long startAt ) {
		next = startAt;
	}
	
	public IDGenerator() { this(1); }
	
	public synchronized long newId() {
		return next++;
	}
}
