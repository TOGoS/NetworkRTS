package togos.networkrts.experimental.sim1.world;

public class StaticPositionFunction implements PositionFunction
{
	long x, y, z;
	
	public StaticPositionFunction( long x, long y, long z ) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public long getX( long ts ) { return x; }
	public long getY( long ts ) { return y; }
	public long getZ( long ts ) { return z; }
}
