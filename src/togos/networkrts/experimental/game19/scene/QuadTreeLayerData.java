package togos.networkrts.experimental.game19.scene;

import togos.networkrts.experimental.game19.world.RSTNode;

public class QuadTreeLayerData
{
	public final RSTNode node;
	public final double size;
	
	public QuadTreeLayerData( RSTNode node, double size ) {
		this.node = node;
		this.size = size;
	}
}
