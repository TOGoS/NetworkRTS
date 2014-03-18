package togos.networkrts.experimental.game19.world.thing.jetman;

class JetManHeadState
{
	public static final float MAX_HEALTH = 0.25f;
	public static final float MAX_BATTERY = 1;
	public static final JetManHeadState DEFAULT = new JetManHeadState(false,MAX_HEALTH,MAX_BATTERY); 
	
	final boolean facingLeft;
	final float health;
	final float battery;
	
	public JetManHeadState( boolean facingLeft, float health, float battery ) {
		this.facingLeft = facingLeft;
		this.health = health;
		this.battery = battery;
	}
	
	public JetManCoreStats getStats() {
		return new JetManCoreStats(
			0, 0,
			0, 0,
			MAX_HEALTH, health,
			MAX_BATTERY, battery
		);
	}
}
