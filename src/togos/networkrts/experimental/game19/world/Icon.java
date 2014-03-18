package togos.networkrts.experimental.game19.world;

import togos.networkrts.experimental.game19.scene.ImageHandle;

public class Icon
{
	public final ImageHandle image;
	public final float imageX, imageY, imageZ, imageWidth, imageHeight;
	public Icon( ImageHandle image, float x, float y, float z, float w, float h ) {
		this.image = image;
		this.imageZ = z;
		this.imageX = x; this.imageWidth  = w;
		this.imageY = y; this.imageHeight = h;
	}
}
