package togos.networkrts.experimental.game19.world.thing;

public class SubstanceQuantity
{
	public final Substance substance;
	/**
	 * How much of the substance their is,
	 * in terms of the substance's unitOfMeasure
	 */
	public final double quantity;
	
	public SubstanceQuantity( Substance substance, double q ) {
		this.substance = substance;
		this.quantity = q;
	}
}
