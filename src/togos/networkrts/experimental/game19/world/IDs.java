package togos.networkrts.experimental.game19.world;

public class IDs
{
	public static final long TYPE_NODE  = 0x0100000000000000l;
	public static final long TYPE_BLOCK = 0x0200000000000000l;
	
	public static final long INDIV_MASK = 0x0000FFFFFFFFFFFFl;
	
	public static final long typeMin( long type ) {
		return type;
	}
	public static final long typeMax( long type ) {
		return type | INDIV_MASK;
	}
}
