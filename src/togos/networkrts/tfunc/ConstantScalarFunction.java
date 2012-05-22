package togos.networkrts.tfunc;

public class ConstantScalarFunction implements ScalarFunction
{
	public static final ConstantScalarFunction ZERO = new ConstantScalarFunction(0);
	
	final double v;
	
	public ConstantScalarFunction( double v ) {
		this.v = v;
	}
	
	@Override public double getValue(long at) {  return v;  }
}
