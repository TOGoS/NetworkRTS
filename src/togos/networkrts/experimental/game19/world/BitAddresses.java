package togos.networkrts.experimental.game19.world;

import togos.networkrts.util.BitAddressUtil;
import togos.networkrts.util.SimpleBitAddressRange;

public class BitAddresses
{
	public static final long TYPE_NODE    = 0x1000000000000000l;
	public static final long TYPE_BLOCK   = 0x2000000000000000l;
	public static final long TYPE_NONTILE = 0x3000000000000000l;
	public static final long TYPE_EXTERNAL= 0x4000000000000000l;
	
	// Shared block/nontile flags
	
	/**
	 * Indicates that other entities should take an object's
	 * physical proximity into account for their own updates.
	 */
	public static final long PHYSINTERACT = 0x0100000000000000l;
	/**
	 * Indicates that an object is in a resting state,
	 * but may become active again if its set of physically
	 * proximate objects changes.
	 * 
	 * If changes happen near a RESTING object, it should be
	 * sent a NEIGHBOR_UPDATED message (this message could
	 * also just be broadcast to entire regions where changes
	 * are going on)
	 * 
	 * If this is set, PHSYINTERACT should also be set.
	 */
	public static final long RESTING      = 0x0200000000000000l;
	// TODO: Determine pick-upability based on mass, size, capacity of picker-upper
	// rather than it being a flag
	// In theory anything could be picked up if the picker-upper is big enough.
	public static final long PICKUP       = 0x0400000000000000l; // May be picked up
	
	public static final long TYPE_MASK    = 0xF000000000000000l;
	public static final long FLAG_MASK    = 0xFFFFFFFF00000000l;
	public static final long ID_MASK      = 0x00000000FFFFFFFFl;
	
	public static final long withMinFlags( int id ) {
		return id & ~FLAG_MASK;
	}
	public static final long withMaxFlags( int id ) {
		return id | FLAG_MASK;
	}
	
	public static final SimpleBitAddressRange requiringTypeAndFlags( long type, long flags ) {
		return new SimpleBitAddressRange(flags, BitAddressUtil.MAX_ADDRESS);
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
