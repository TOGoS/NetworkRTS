package togos.networkrts.s64;

import java.awt.Color;

import togos.blob.ByteChunk;
import togos.networkrts.simpleclient.BackgroundType;

public class Block implements BackgroundType
{
	private static final long serialVersionUID = 1L;
	
	public static final int FLAG_WALKABLE   = 0x01;
	public static final int FLAG_BOATABLE   = 0x02;
	public static final int FLAG_BLOCKING   = 0x04;
	public static final int FLAG_OPAQUE     = 0x08;
	
	public final ByteChunk entityId;
	public final int flags;
	public final Color color;
	
	public Block( ByteChunk entityId, int flags, Color color ) {
		this.entityId = entityId;
		this.flags = flags;
		this.color = color;
	}
	
	public Color getColor() {  return color;  }
	
	// Could move these things outside block and look them up by hash:
	
	protected GridNode64 recursiveNode;
	public synchronized GridNode64 getRecursiveNode() {
		if( recursiveNode == null ) {
			recursiveNode = GridNode64.createRecursiveNode( getStack() );
		}
		return recursiveNode;
	}
	
	protected Block[] stack;
	public synchronized Block[] getStack() {
		if( stack == null ) {
			stack = new Block[]{ this };
		}
		return stack;
	}
}
