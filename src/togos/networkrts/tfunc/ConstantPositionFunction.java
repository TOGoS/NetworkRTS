package togos.networkrts.tfunc;

public class ConstantPositionFunction implements PositionFunction
{
	final double x, y, z;
	
	public ConstantPositionFunction( double x, double y, double z ) {
		this.x = x; this.y = y; this.z = z;
	}
	
	@Override
	public void getPosition(long timestamp, double[] dest) {
		dest[0] = x; dest[1] = y; dest[2] = z;
	}
}
