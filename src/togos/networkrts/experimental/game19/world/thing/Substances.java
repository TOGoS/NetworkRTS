package togos.networkrts.experimental.game19.world.thing;

import java.util.UUID;

public class Substances
{
	static final Substance KEROSENE = new Substance( UUID.fromString("5e476460-b5d7-11e3-a5e2-0800200c9a66"), "kerosene", SubstanceUnit.KILOGRAM, 1, 1250d/1000000 );

	private Substances() { }
}
