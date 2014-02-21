package togos.networkrts.experimental.game19.world;

import togos.networkrts.util.BitAddressUtil;
import togos.networkrts.util.SimpleBitAddressRange;

public class BitAddresses
{
	public static final long TYPE_NODE    = 0x1000000000000000l;
	public static final long TYPE_BLOCK   = 0x2000000000000000l;
	
	// Block flags
	public static final long BLOCK_SOLID  = 0x0000000100000000l;
	public static final long BLOCK_OPAQUE = 0x0000000200000000l;
	public static final long BLOCK_PHYS   = 0x0000000400000000l; // May need physics update
	
	public static final long TYPE_MASK    = 0xF000000000000000l;
	public static final long FLAG_MASK    = 0xFFFFFFFF00000000l;
	public static final long ID_MASK      = 0x00000000FFFFFFFFl;
	
	public static final long withMinFlags( int id ) {
		return id & ~FLAG_MASK;
	}
	public static final long withMaxFlags( int id ) {
		return id | FLAG_MASK;
	}
	
	/** Range of addresses that include all of the given flag(s) */
	public static final SimpleBitAddressRange requiringFlags( long flags ) {
		return new SimpleBitAddressRange(flags, BitAddressUtil.MAX_ADDRESS);
	}
	
	public static final long forceType( long type, long flags ) {
		assert (type & TYPE_MASK) == type;
		return (flags & ~TYPE_MASK) | type; 
	}
	
	public static final long makeAddress( long flags, int id ) {
		return (flags&FLAG_MASK) | id;
	}
	
	public static final int extractId( long address ) {
		return (int)(address & ID_MASK);
	}
}
