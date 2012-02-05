package togos.networkrts.simulation;

import togos.networkrts.simulation.part.Brick;
import togos.networkrts.world.WorldObject;

/**
 * Extends WorldObject to add fields to track internal state.
 */
public class SimulationObject extends WorldObject
{
	public Part rootPart = new Brick( 1000 );
}
