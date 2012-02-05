package togos.networkrts.simulation.event;

public class Shockwave extends SimulationEvent
{
	long originX, originY, originZ;
	int speed; // mm/second
	int rad; // radius at which it disappears
	int damage; // damage per area
	int color;
	int colorIntensity; // alpha = original alpha * colorIntensity/(r*r)
	
	public Shockwave( long ts ) {
		super(ts);
	}
}
