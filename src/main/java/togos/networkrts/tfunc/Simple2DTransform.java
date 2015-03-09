package togos.networkrts.tfunc;

public final class Simple2DTransform implements Simple2DTransformFunction
{
	public static final Simple2DTransform IDENTITY = new Simple2DTransform();
	
	public double x, y, z;
	public double rotation;
	public double scale;
	
	public Simple2DTransform( double x, double y, double z, double rotation, double scale ) {
		this.x = x; this.y = y; this.z = z;
		this.rotation = rotation; this.scale = scale;
	}
	
	public Simple2DTransform( double x, double y, double z, double rotation ) {
		this( x, y, z, rotation, 1 );
	}
	
	public Simple2DTransform() {
		this( 0, 0, 0, 0 );
	}
	
	@Override
	public void getTransform( long timestamp, Simple2DTransform dest ) {
		dest.x = x; dest.y = y; dest.z = z;
		dest.rotation = rotation;
		dest.scale = scale;
	}
	
	public static Simple2DTransform from( Simple2DTransformFunction tf, long ts ) {
		assert tf != null;
		Simple2DTransform xf = new Simple2DTransform();
		tf.getTransform( ts, xf );
		return xf;
	}
}
