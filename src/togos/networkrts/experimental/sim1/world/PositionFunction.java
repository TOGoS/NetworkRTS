package togos.networkrts.experimental.sim1.world;

public interface PositionFunction
{
	public long getX(long ts);
	public long getY(long ts);
	public long getZ(long ts);
}
