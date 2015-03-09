package togos.networkrts.experimental.game19.world.thing.jetman;

public class JetManCoreStats
{
	public final float maxSuitHealth;
	public final float suitHealth;
	public final float maxFuel;
	public final float fuel;
	public final float maxHelmetHealth;
	public final float helmetHealth;
	public final float batteryCharge;
	public final float maxBatteryCharge;
	
	public JetManCoreStats(
		float maxSuit, float suit,
		float maxFuel, float fuel,
		float maxHelmet, float helmet,
		float maxBattery, float battery
	) {
		this.maxSuitHealth = maxSuit;
		this.suitHealth = suit;
		this.maxFuel = maxFuel;
		this.fuel = fuel;
		this.maxHelmetHealth = maxHelmet;
		this.helmetHealth = helmet;
		this.maxBatteryCharge = maxBattery;
		this.batteryCharge  = battery;
	}
}
