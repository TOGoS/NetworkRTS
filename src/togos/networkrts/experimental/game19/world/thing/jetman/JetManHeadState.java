package togos.networkrts.experimental.game19.world.thing.jetman;

class JetManHeadState {
	public static final JetManHeadState DEFAULT = new JetManHeadState(false,0.25,1); 
	
	final boolean facingLeft;
	final double health;
	final double battery;
	
	public JetManHeadState( boolean facingLeft, double health, double battery ) {
		this.facingLeft = facingLeft;
		this.health = health;
		this.battery = battery;
	}
}
