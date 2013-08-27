package togos.networkrts.experimental.dungeon;

import java.io.Serializable;

class Projection implements Serializable
{
	private static final long serialVersionUID = 2920724968130479539L;
	
	public final BlockField blockField;
	public Projection( int w, int h, int d ) {
		blockField = new BlockField( w, h, d, Block.EMPTY_STACK );
	}
	
	/** Position of the origin within the block field */
	float originX, originY;
	
	public void projectFrom( Room r, float x, float y, float z ) {
		Raycast.raycastXY( r, x, y, (int)z, blockField, blockField.w/2, blockField.h/2 );
		this.originX = blockField.w/2f;
		this.originY = blockField.h/2f;
	}
	
	public void projectFrom(CellCursor pos) {
		projectFrom(pos.room, pos.x, pos.y, (int)pos.z);
	}
}
