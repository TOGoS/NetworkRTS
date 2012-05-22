package togos.networkrts.experimental.entree;

/**
 * Any object that knows its own 3D position and rotation
 */
public interface WorldPositioned
{
	public void getPosition( long t, double[] dest );
	public double getRotation( long t );
}
