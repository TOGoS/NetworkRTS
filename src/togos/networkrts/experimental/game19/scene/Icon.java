package togos.networkrts.experimental.game19.scene;


public class Icon
{
	public static final Float DEFAULT_NONTILE_FRONT_Z = 0.1f;
	public static final Float DEFAULT_BLOCK_FRONT_Z = 0.5f;
	
	public final ImageHandle image;
	public final float imageX, imageY, imageZ, imageWidth, imageHeight;
	public Icon( ImageHandle image, float x, float y, float z, float w, float h ) {
		this.image = image;
		this.imageZ = z;
		this.imageX = x; this.imageWidth  = w;
		this.imageY = y; this.imageHeight = h;
	}
}
