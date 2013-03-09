package togos.networkrts.experimental.gameengine1.index;

/**
 * An object defined by a bounding box, optional tag, and flags.
 */
public class Entity extends AABB
{
	/** Can be used to reference an entity regardless of its state */ 
	public final Object tag;
	public final long flags;
	
	public Entity( Object tag, long flags, double minX, double minY, double minZ, double maxX, double maxY, double maxZ ) {
		super( minX, minY, minZ, maxX, maxY, maxZ );
		this.tag = tag;
		this.flags = flags;
	}
}
