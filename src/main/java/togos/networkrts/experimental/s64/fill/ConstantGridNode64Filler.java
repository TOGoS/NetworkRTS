package togos.networkrts.experimental.s64.fill;

import togos.networkrts.experimental.s64.GridNode64;

public class ConstantGridNode64Filler implements GridNode64Filler
{
	protected GridNode64 node;
	
	public ConstantGridNode64Filler( GridNode64 node ) {
		this.node = node;
	}
	
	public GridNode64 getNode(double x, double y, double size) {
		return node;
	}
}
