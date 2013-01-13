package togos.networkrts.experimental.dungeon;

import java.awt.Color;

class Block {
	public static final Block[] EMPTY_STACK = new Block[0];
	
	final long blockId;
	final float opacity;
	final Color color;
	final boolean blocking;
	
	public Block( long blockId, Color c, float opacity, boolean blocking ) {
		this.blockId = blockId;
		this.color = c;
		this.opacity = opacity;
		this.blocking = blocking;
	}
	
	public final Block[] stack = new Block[] { this };
	
	public static final Block FLOOR = new Block( 100, Color.GRAY, 0, false );
	public static final Block GRASS = new Block( 101, Color.GREEN, 0, false );
	public static final Block WALL = new Block( 102, Color.WHITE, 1, true );
	public static final Block PLAYER = new Block( 103, Color.YELLOW, 0, true );
}
