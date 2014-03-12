package togos.networkrts.experimental.game19.world.thing.jetman;

class JetManState {
	public static final JetManState DEFAULT = new JetManState(0,0,false); 
	
	final int walkState, thrustDir;
	final boolean facingLeft;
	public JetManState( int walkState, int thrustDir, boolean facingLeft ) {
		this.walkState = walkState;
		this.thrustDir = thrustDir;
		this.facingLeft = facingLeft;
	}
}
