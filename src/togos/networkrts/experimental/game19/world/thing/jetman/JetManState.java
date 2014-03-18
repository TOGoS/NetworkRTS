package togos.networkrts.experimental.game19.world.thing.jetman;

class JetManState
{
	public static final JetManState DEFAULT = new JetManState(
		0, -1, false,
		1, 100,
		JetManHeadState.DEFAULT
	); 
	
	final int walkState, thrustDir;
	final boolean facingLeft;
	final float suitHealth, fuel;
	final JetManHeadState headState;
	
	public JetManState(
		int walkState, int thrustDir, boolean facingLeft,
		float suitHealth, float fuel,
		JetManHeadState headState
	) {
		this.walkState = walkState;
		this.thrustDir = thrustDir;
		this.facingLeft = facingLeft;
		this.suitHealth = suitHealth;
		this.fuel = fuel;
		this.headState = headState;
	}
	
	public JetManCoreStats getStats() {
		return new JetManCoreStats(
			1  , suitHealth,
			100, fuel,
			JetManHeadState.MAX_HEALTH, headState.health,
			JetManHeadState.MAX_BATTERY, headState.battery
		);
	}
}
