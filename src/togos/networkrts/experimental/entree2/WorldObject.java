package togos.networkrts.experimental.entree2;

public abstract class WorldObject {
	public final double x, y;
	
	/* The following functions MUST ALWAYS RETURN THE SAME VALUE FOR A GIVEN OBJECT */
	
	/**
	 * Return the time at which this object needs attention
	 * Long.MAX_VALUE indicates an object never auto-updates
	 * */
	public abstract long getAutoUpdateTime();
	public abstract long getFlags();
	public abstract double getMaxRadius();
	
	public WorldObject( double x, double y ) {
		this.x = x; this.y = y;
	}
}
