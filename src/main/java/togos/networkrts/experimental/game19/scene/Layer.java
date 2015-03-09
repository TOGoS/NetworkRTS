package togos.networkrts.experimental.game19.scene;

import togos.networkrts.util.ResourceHandle;

public class Layer
{
	public static class VisibilityClip {
		public final double minX, minY, maxX, maxY;
		public VisibilityClip( double minX, double minY, double maxX, double maxY ) {
			this.minX = minX; this.minY = minY;
			this.maxX = maxX; this.maxY = maxY;
		}
	}
	
	public static class LayerLink {
		public final boolean isBackground;
		public final ResourceHandle<Layer> layer;
		public final double offsetX, offsetY;
		public final double distance;
		public final int altColor;
		public LayerLink( boolean isBackground, ResourceHandle<Layer> layer, double offsetX, double offsetY, double distance, int altColor ) {
			this.isBackground = isBackground;
			this.layer = layer;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.distance = distance;
			this.altColor = altColor;
		}
	}
	
	public final Object data;
	/** Offset of the top left corner of the data from layer's origin */
	public final double dataOffsetX, dataOffsetY;
	public final LayerLink next;
	
	public Layer( Object data, double dox, double doy, LayerLink next ) {
		this.data = data; this.dataOffsetX = dox; this.dataOffsetY = doy;
		this.next = next;
	}
}
