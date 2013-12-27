package togos.networkrts.experimental.qt2drender;

public class Sprite {
	final float x, y, z, w, h;
	// TODO: Should be an animation
	final ImageHandle image;
	
	public Sprite( float x, float y, float z, ImageHandle image, float w, float h ) {
		this.x = x; this.y = y;
		this.z = z;
		this.image = image;
		this.w = w; this.h = h;
	}
}