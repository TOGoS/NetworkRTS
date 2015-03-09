package togos.networkrts.experimental.rocopro.demo;

public class CamView
{
	/** Offset of the view grid relative to the camera */
	public final float offsetX, offsetY;
	public final int width, height;
	public final byte[] data;
	
	public CamView( float x, float y, int w, int h ) {
		this.offsetX = x;
		this.offsetY = y;
		this.width = w;
		this.height = h;
		this.data = new byte[w*h]; // RLE might be nice here
	}
}
