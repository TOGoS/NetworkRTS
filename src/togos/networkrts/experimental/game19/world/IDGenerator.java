package togos.networkrts.experimental.game19.world;

public class IDGenerator {
	long next;
	
	public IDGenerator( long startAt ) {
		next = startAt;
	}
	
	public IDGenerator() { this(1); }
	
	public synchronized long newId( long type ) {
		return (next++) | type;
	}
	
	public long newBlockId() { return newId( IDs.TYPE_BLOCK ); }
}
