package togos.networkrts.experimental.dungeon;

import java.awt.Color;

class Block {
	public static final Block[] EMPTY_STACK = new Block[0];
	
	final float opacity;
	final Color color;
	
	public Block( Color c, float opacity ) {
		this.color = c;
		this.opacity = opacity;
	}
	
	public final Block[] stack = new Block[] { this };
	
	public static final Block FLOOR = new Block( Color.GRAY, 0 );
	public static final Block GRASS = new Block( Color.GREEN, 0 );
	public static final Block WALL = new Block( Color.WHITE, 1 );
	public static final Block PLAYER = new Block( Color.YELLOW, 0 );
}
