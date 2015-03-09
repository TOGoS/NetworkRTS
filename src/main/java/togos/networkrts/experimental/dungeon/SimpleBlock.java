package togos.networkrts.experimental.dungeon;

import java.awt.Color;

class SimpleBlock implements Block
{
	public static final Block[] EMPTY_STACK = new Block[0];
	
	final float opacity;
	final Color color;
	final boolean blocking;
	
	public final Block[] stack = new Block[] { this };
	
	public SimpleBlock( Color c, float opacity, boolean blocking ) {
		this.color = c;
		this.opacity = opacity;
		this.blocking = blocking;
	}
	
	@Override public Color getColor() { return color; }
	@Override public String getDescription() { return "some block"; }
	@Override public float getOpacity() { return opacity; }
	@Override public boolean isBlocking() { return blocking; }
	@Override public Block[] getStack() { return stack; }
	
	public static final SimpleBlock FLOOR = new SimpleBlock( Color.GRAY, 0, false );
	public static final SimpleBlock GRASS = new SimpleBlock( Color.GREEN, 0, false );
	public static final SimpleBlock WALL = new SimpleBlock( Color.WHITE, 1, true );
	public static final SimpleBlock GRATING = new SimpleBlock( Color.GREEN, 1, true );
	public static final SimpleBlock PLAYER = new SimpleBlock( Color.YELLOW, 0, true );
	public static final SimpleBlock BOT = new SimpleBlock( Color.RED, 0, true );
	public static final SimpleBlock FOLIAGE = new SimpleBlock( new Color(0,0.5f,0), 0.4f, false );
}
