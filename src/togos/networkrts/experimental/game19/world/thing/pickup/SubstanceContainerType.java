package togos.networkrts.experimental.game19.world.thing.pickup;

import togos.networkrts.experimental.game19.scene.Icon;
import togos.networkrts.experimental.gameengine1.index.AABB;

public class SubstanceContainerType
{
	public final String description;
	/**
	 * A series of icons where 0 represents empty, and n-1 represents full 
	 */
	public final Icon[] icons;
	public final AABB relativePhysicalAabb;
	public final double emptyMass;
	public final double internalVolume;
	// Leakiness?
	// Max. internal/external pressure?
	// What does it become when broken?
	
	public SubstanceContainerType( String desc, Icon[] icons, AABB aabb, double mass, double volume ) {
		this.description = desc;
		this.icons = icons;
		this.relativePhysicalAabb = aabb;
		this.emptyMass = mass;
		this.internalVolume = volume;
	}
}
