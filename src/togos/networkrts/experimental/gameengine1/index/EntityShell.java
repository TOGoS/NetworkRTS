package togos.networkrts.experimental.gameengine1.index;

/**
 * Provides methods for things an entity can do to the world. 
 */
public interface EntityShell<EC extends Entity>
{
	public void add( EC e );
}