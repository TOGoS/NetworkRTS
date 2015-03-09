package togos.networkrts.experimental.game19.world;

public interface HasPositionAndVelocity
{
	/**
	 * Return the timestamp at which x and y are accurate.
	 */
	public long getReferenceTime();
	public double getX();
	public double getY();
	public double getVelocityX();
	public double getVelocityY();
}
