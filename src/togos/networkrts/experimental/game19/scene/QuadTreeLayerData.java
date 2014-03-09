package togos.networkrts.experimental.game19.scene;

import togos.networkrts.experimental.game19.world.WorldNode;

public class QuadTreeLayerData
{
	public final WorldNode node;
	public final double size;
	
	public QuadTreeLayerData( WorldNode node, double size ) {
		this.node = node;
		this.size = size;
	}
}
