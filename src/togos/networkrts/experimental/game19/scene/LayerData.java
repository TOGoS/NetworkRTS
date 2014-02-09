package togos.networkrts.experimental.game19.scene;

import togos.networkrts.experimental.game19.world.BlockStack;


public class LayerData {
	/** Width and height in cell units */
	public final int width, height, depth;
	public final BlockStack[] blockStacks;
	
	public LayerData( int w, int h, int d, BlockStack[] blockStacks ) {
		this.width = w; this.height = h; this.depth = d;
		this.blockStacks = blockStacks;
	}
}
