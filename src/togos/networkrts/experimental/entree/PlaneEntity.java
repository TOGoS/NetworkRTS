package togos.networkrts.experimental.entree;

public interface PlaneEntity
{
	public static final int FLAG_EXISTS = 0x1;
	
	public Object getId();
	public Object getPlaneId();
	public double getX();
	public double getY();
	/** Maximum physical or visual radius; the entity may be completely ignored by events that occur outside of this */
	public double getMaxRadius();
	public int getFlags();
}
