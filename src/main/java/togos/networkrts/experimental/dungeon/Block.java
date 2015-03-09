package togos.networkrts.experimental.dungeon;

import java.awt.Color;

public interface Block
{
	public String getDescription();
	public float getOpacity();
	public Color getColor();
	public boolean isBlocking();
	
	/** Return the stack containing only this block. */
	public Block[] getStack();
}
