package togos.networkrts.experimental.game19.world.thing;

public enum SubstanceUnit
{
	KILOGRAM("kilogram", "kg");
	
	public final String name, abbreviation;
	private SubstanceUnit( String name, String abbreviation ) {
		this.name = name;
		this.abbreviation = abbreviation;
	}
}
