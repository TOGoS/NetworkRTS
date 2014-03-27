package togos.networkrts.experimental.game19.world.thing.jetman;

class JetManState
{
	public static final JetManState DEFAULT = new JetManState(
		0, -1, 0,
		1, 100,
		JetManHeadState.DEFAULT
	); 
	
	public static final int S_FACING_LEFT = 0x01;
	public static final int S_BACK_THRUSTER_ON = 0x02;
	public static final int S_BOTTOM_THRUSTER_ON = 0x04;
	public static final int S_FEET_ON_GROUND = 0x8;
	
	final int walkFrame;
	final int thrustDir;
	final int state;
	final float suitHealth, fuel;
	final JetManHeadState headState;
	
	public boolean checkState(int flag) {
		return (state & flag) == flag;
	}
	
	public JetManState(
		int walkFrame, int thrustDir, int state,
		float suitHealth, float fuel,
		JetManHeadState headState
	) {
		this.walkFrame = walkFrame;
		this.thrustDir = thrustDir;
		this.state = state;
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
