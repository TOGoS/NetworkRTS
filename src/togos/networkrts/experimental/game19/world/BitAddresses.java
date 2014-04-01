package togos.networkrts.experimental.game19.world;

/**
 * Game19 bit addresses are divided into flags, type, and ID.
 * For a given object, flags are assumed to be dynamic, type
 * relatively stable, and ID is permanent.
 * 
 * An object might
 * switch between being a nontile and a block, but most
 * interactions will not need to take this possibility into
 * account, so messages to a particular object can be usually
 * be sent to a range where only flags differ between the
 * upper and lower bounds, but type and ID are the same. 
 */
public class BitAddresses
{
	// Shared block/nontile flags
	
	public static final int  FLAG_SHIFT   = 48;
	public static final long FLAG_MASK    = 0xFFFF000000000000l;

	/**
	 * Indicates that other entities should take an object's
	 * physical proximity into account for their own updates.
	 */
	public static final long PHYSINTERACT = 0x0001000000000000l;
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
	public static final long RESTING      = 0x0002000000000000l;
	// TODO: Determine pick-upability based on mass, size, capacity of picker-upper
	// rather than it being a flag
	// In theory anything could be picked up if the picker-upper is big enough.
	public static final long PICKUP       = 0x0004000000000000l; // May be picked up
	public static final long UPPHASE1     = 0x0010000000000000l;
	public static final long UPPHASE2     = 0x0020000000000000l;
	
	public static final int  TYPE_SHIFT   = 44;
	public static final long TYPE_MASK    = 0x0000F00000000000l;
	
	public static final long TYPE_NODE    = 0x0000100000000000l;
	public static final long TYPE_BLOCK   = 0x0000200000000000l;
	public static final long TYPE_NONTILE = 0x0000300000000000l;
	// Miscellaneous simulated components
	public static final long TYPE_INTERNAL= 0x0000400000000000l;
	// Stuff that lives outside the simulation event loop
	public static final long TYPE_EXTERNAL= 0x0000500000000000l;
	
	public static final long ID_MASK      = 0x00000FFFFFFFFFFFl;
	
	// Since flags can change over time, you probably want
	// to use a range that includes all of them when addressing
	// the same object at a different point im time.
	
	public static final long withMinFlags( long typeAndId ) {
		return typeAndId & ~FLAG_MASK;
	}
	public static final long withMaxFlags( long typeAndId ) {
		return typeAndId | FLAG_MASK;
	}
	
	public static final long forceType( long type, long rest ) {
		assert (rest & TYPE_MASK) == type;
		return (rest & ~TYPE_MASK) | type;
	}
	
	public static final long makeAddress( long flags, long id ) {
		return (flags&FLAG_MASK) | id;
	}
	
	public static final long extractType( long address ) {
		return address & TYPE_MASK;
	}
	
	public static final long extractId( long address ) {
		return address & ID_MASK;
	}
	
	public static String toString( long address ) {
		long id = address & ID_MASK;
		int flags = (int)((address & FLAG_MASK) >>> FLAG_SHIFT);
		int type  = (int)((address & TYPE_MASK) >>> TYPE_SHIFT);
		return String.format("%04x-%1x-%011x", flags, type, id);
	}
	
	/**
	 * Return the address flag indicating that an object
	 * requires update at its next auto update time
	 * for the given phase
	 */
	public static long phaseUpdateFlag( int phase ) {
		switch( phase ) {
		case 1: return UPPHASE1;
		case 2: return UPPHASE2;
		default: throw new RuntimeException("Invalid phase: "+phase);
		}
	}
	
	public static boolean containsFlag( long address, long flag ) {
		return (address & flag) == flag;
	}
}
