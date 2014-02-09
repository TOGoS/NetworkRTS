package togos.networkrts.experimental.game19.scene;


public class Layer {
	public final LayerData data;
	/** Offset of the top left corner of the data from layer's origin */
	public final double dataOffsetX, dataOffsetY;
	public final Layer next;
	public final double nextOffsetX, nextOffsetY;
	public final double nextParallaxDistance;
	
	public Layer( LayerData data, double dox, double doy, Layer next, double nox, double noy, double npd ) {
		this.data = data; this.dataOffsetX = dox; this.dataOffsetY = doy;
		this.next = next; this.nextOffsetX = nox; this.nextOffsetY = noy;
		this.nextParallaxDistance = npd;
	}
}
