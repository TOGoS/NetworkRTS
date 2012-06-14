package togos.networkrts.experimental.entree2;

public abstract class WorldObject {
	public final double x, y, maxRadius;
	/**
	 * Return the time at which this object needs attention
	 * Long.MAX_VALUE indicates an object never auto-updates
	 * */
	public abstract long getAutoUpdateTime();
	public abstract long getFlags();
	
	public WorldObject( double x, double y, double rad ) {
		this.x = x; this.y = y;
		this.maxRadius = rad;
	}
}
