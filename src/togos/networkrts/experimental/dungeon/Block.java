package togos.networkrts.experimental.dungeon;

import java.awt.Color;

class Block
{
	public static final Block[] EMPTY_STACK = new Block[0];
	
	final float opacity;
	final Color color;
	final boolean blocking;
	
	public Block( Color c, float opacity, boolean blocking ) {
		this.color = c;
		this.opacity = opacity;
		this.blocking = blocking;
	}
	
	public final Block[] stack = new Block[] { this };
	
	public static final Block FLOOR = new Block( Color.GRAY, 0, false );
	public static final Block GRASS = new Block( Color.GREEN, 0, false );
	public static final Block WALL = new Block( Color.WHITE, 1, true );
	public static final Block GRATING = new Block( Color.GREEN, 1, true );
	public static final Block PLAYER = new Block( Color.YELLOW, 0, true );
	public static final Block BOT = new Block( Color.RED, 0, true );
	public static final Block FOLIAGE = new Block( new Color(0,0.5f,0), 0.4f, false );
}
