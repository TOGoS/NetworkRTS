package togos.networkrts.experimental.game19.scene;

public class Layer
{
	public static class VisibilityClip {
		public final double minX, minY, maxX, maxY;
		public VisibilityClip( double minX, double minY, double maxX, double maxY ) {
			this.minX = minX; this.minY = minY;
			this.maxX = maxX; this.maxY = maxY;
		}
	}
	
	public final Object data;
	/** Offset of the top left corner of the data from layer's origin */
	public final double dataOffsetX, dataOffsetY;
	
	/**
	 * Section of the layer that is visible
	 * (offsets are relative to the layer's origin)
	 **/
	public final VisibilityClip visibilityClip;
	
	public final boolean nextIsBackground;
	public final Layer next;
	public final double nextOffsetX, nextOffsetY;
	public final double nextParallaxDistance;
	
	public Layer( Object data, double dox, double doy, VisibilityClip visibilityClip, boolean nextIsBackground, Layer next, double nox, double noy, double npd ) {
		this.visibilityClip = visibilityClip;
		this.nextIsBackground = nextIsBackground;
		this.data = data; this.dataOffsetX = dox; this.dataOffsetY = doy;
		this.next = next; this.nextOffsetX = nox; this.nextOffsetY = noy;
		this.nextParallaxDistance = npd;
	}
}
