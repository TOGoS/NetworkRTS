package togos.networkrts.experimental.ubtp;

import togos.networkrts.experimental.tcp1.PacketHandler;

/**
 * Accepts UBTP-encoded packets, extracts the packets encoded within,
 * and passes to another PacketHandler
 */
public class UBTPPacketHandler implements PacketHandler
{
	final PacketHandler blockHandler;
	
	public UBTPPacketHandler( PacketHandler blockHandler ) {
		this.blockHandler = blockHandler;
	}
	
	protected void logInvalidPacket( String m ) {
		System.err.println("UBTP decode error: "+m);
		throw new RuntimeException("UBTP decode error: "+m);
	}
	
	@Override public void handlePacket( byte[] data, int offset, int length ) {
		while( offset < length ) {
			byte opcode = data[offset++];
			switch( opcode ) {
			case UBTP.OP_WHOLEBLOCK: {
				if( offset+2 > length ) {
					logInvalidPacket("WHOLEBLOCK segment doesn't have enough space ("+offset+"/"+length+") in packet to encode block size");
					return;
				}
				byte sizeHigh = data[offset++];
				byte sizeLow  = data[offset++];
				int blockSize = ((sizeHigh<<8) | (0xFF&sizeLow)) & 0xFFFF;
				if( offset+blockSize > length ) {
					logInvalidPacket("WHOLEBLOCK segment ("+offset+"+"+blockSize+"/"+length+") extends past end of packet");
					return;
				}
				blockHandler.handlePacket(data, offset, blockSize);
				offset += blockSize;
				break;
			}
			default:
				logInvalidPacket("Unrecognized opcode "+opcode+" at offset "+(offset-1));
				return;
			}
		}
	}
}
