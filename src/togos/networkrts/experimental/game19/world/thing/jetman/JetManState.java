package togos.networkrts.experimental.game19.world.thing.jetman;

class JetManState
{
	public static final JetManState DEFAULT = new JetManState(0,-1,false,1,100); 
	
	final int walkState, thrustDir;
	final boolean facingLeft;
	final double suitHealth, fuel;
	
	public JetManState( int walkState, int thrustDir, boolean facingLeft, double suitHealth, double fuel ) {
		this.walkState = walkState;
		this.thrustDir = thrustDir;
		this.facingLeft = facingLeft;
		this.suitHealth = suitHealth;
		this.fuel = fuel;
	}
}
