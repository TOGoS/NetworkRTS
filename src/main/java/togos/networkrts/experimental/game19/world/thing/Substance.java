package togos.networkrts.experimental.game19.world.thing;

import java.util.UUID;

public class Substance
{
	public final UUID uuid;
	public final String name;
	public final SubstanceUnit unitOfMeasure;
	// Could add density, viscosity, etc
	/** unit mass, in kilograms */
	public final double unitMass;
	/** unit volume, in cubic meters */
	public final double unitVolume;
	
	public Substance( UUID uuid, String name, SubstanceUnit unitOfMeasure, double unitMass, double unitVolume ) {
		this.uuid = uuid;
		this.name = name;
		this.unitOfMeasure = unitOfMeasure;
		this.unitMass = unitMass;
		this.unitVolume = unitVolume;
	}
	
	@Override public boolean equals(Object obj) {
		return obj instanceof Substance && uuid.equals( ((Substance)obj).uuid );
	}
	
	@Override public int hashCode() {
		return uuid.hashCode();
	}
}
