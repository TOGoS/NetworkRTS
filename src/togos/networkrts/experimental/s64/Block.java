package togos.networkrts.experimental.s64;

import togos.blob.ByteChunk;
import togos.networkrts.experimental.simpleclient.BackgroundType;
import togos.networkrts.tfunc.ColorFunction;

public class Block implements BackgroundType
{
	public static final Block[] EMPTY_STACK = new Block[0];
	
	private static final long serialVersionUID = 1L;
	
	public static final int FLAG_WALKABLE   = 0x01;
	public static final int FLAG_BOATABLE   = 0x02;
	public static final int FLAG_BLOCKING   = 0x04;
	public static final int FLAG_OPAQUE     = 0x08;
	
	public final ByteChunk entityId;
	public final int flags;
	public final ColorFunction color;
	
	public Block( ByteChunk entityId, int flags, ColorFunction color ) {
		this.entityId = entityId;
		this.flags = flags;
		this.color = color;
	}
	
	public ColorFunction getColorFunction() {  return color;  }
	
	// Could move these things outside block and look them up by hash:
	
	protected GridNode64 recursiveNode;
	public synchronized GridNode64 getRecursiveNode() {
		if( recursiveNode == null ) {
			recursiveNode = new HomogeneousGridNode64( getStack() );
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
