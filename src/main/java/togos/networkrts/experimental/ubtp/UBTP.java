package togos.networkrts.experimental.ubtp;

/**
 * Unreliable block transfer protocol.
 * Transfers blocks over datagrams.
 * Multiple small blocks can be packed into one datagram. 
 * Probability of large blocks arriving intact over an unreliable datagram channel decreases
 * as blocks span more packets.
 */
public class UBTP
{
	// Data is the remainder of the packet, including this zero byte
	public static final byte OP_TREELEAF = 0;
	// Followed by
	//  16-bit segment length
	public static final byte OP_WHOLEBLOCK = 32;
	// Followed by
	//  32-bit CRC
	//  24-bit block length
	//  24-bit segment offset
	//  16-bit segment length
	public static final byte OP_SEGMENT = 33;
	
	private UBTP() { }
}
