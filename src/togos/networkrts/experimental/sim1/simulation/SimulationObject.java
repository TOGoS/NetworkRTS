package togos.networkrts.experimental.sim1.simulation;

import togos.networkrts.experimental.sim1.simulation.part.Brick;
import togos.networkrts.experimental.sim1.world.WorldObject;

/**
 * Extends WorldObject to add fields to track internal state.
 */
public class SimulationObject extends WorldObject
{
	public Part rootPart = new Brick( 1000 );
}
