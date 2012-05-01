package togos.networkrts.experimental.entree;

public class EntityPlaneUpdate
{
	public static final PlaneEntity[] EMPTY_ENTITY_LIST = new PlaneEntity[0];
	
	public final PlaneEntity[] remove;
	public final int removeCount; 
	public final PlaneEntity[] add;
	public final int addCount;
	
	public EntityPlaneUpdate( PlaneEntity[] remove, int removeCount, PlaneEntity[] add, int addCount ) {
		this.remove      = remove;
		this.removeCount = removeCount;
		this.add         = add;
		this.addCount    = addCount;
	}
	
	public EntityPlaneUpdate( PlaneEntity[] remove, PlaneEntity[] add ) {
		this( remove, remove.length, add, add.length );
	}
	
	public EntityPlaneUpdate() {
		this( EMPTY_ENTITY_LIST, EMPTY_ENTITY_LIST );
	}
}
