package togos.networkrts.experimental.ubtp;

import togos.blob.util.BlobUtil;
import togos.networkrts.experimental.tcp1.PacketHandler;

/**
 * Accepts UBTP-encoded packets, extracts the packets encoded within,
 * and passes to another PacketHandler
 */
public class UBTPPacketHandler implements PacketHandler
{
	final PacketHandler blockHandler;
	final int[] blockCrcs;
	final byte[][] blockData;
	final int[] blockOffsets;
	int nextBlockSlot = 0;
	
	public UBTPPacketHandler( PacketHandler blockHandler ) {
		this.blockHandler = blockHandler;
		int blockCacheSize = 4;
		this.blockCrcs = new int[blockCacheSize];
		this.blockOffsets = new int[blockCacheSize];
		this.blockData = new byte[blockCacheSize][];
		for( int i=0; i<blockCacheSize; ++i ) blockData[i] = BlobUtil.EMPTY_BYTE_ARRAY;
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
			case UBTP.OP_SEGMENT: {
				if( offset+4+3+3+2 > length ) {
					logInvalidPacket("SEGMENT segment doesn't have enough space ("+offset+"/"+length+") in packet to encode header");
					return;
				}
				byte a,b,c,d;
				a = data[offset++]; b = data[offset++];
				c = data[offset++]; d = data[offset++];
				int crc = ((a&0xFF)<<24) | ((b&0xFF)<<16) | ((c&0xFF)<<8) | ((d&0xFF)<<0);
				b = data[offset++]; c = data[offset++]; d = data[offset++];
				int blockLength = ((b&0xFF)<<16) | ((c&0xFF)<<8) | ((d&0xFF)<<0);
				b = data[offset++]; c = data[offset++]; d = data[offset++];
				int segmentOffset = ((b&0xFF)<<16) | ((c&0xFF)<<8) | ((d&0xFF)<<0);
				c = data[offset++]; d = data[offset++];
				int segmentLength = ((c&0xFF)<<8) | ((d&0xFF)<<0);
				int blockIdx;
				findBlockCache: {
					for( int i=0; i<blockCrcs.length; ++i ) {
						if( blockCrcs[i] == crc && blockData[i].length == blockLength ) {
							blockIdx = i;
							break findBlockCache;
						}
					}
					blockIdx = nextBlockSlot++;
					if( nextBlockSlot >= blockCrcs.length ) nextBlockSlot = 0;
					blockOffsets[blockIdx] = 0;
					blockCrcs[blockIdx] = crc;
					blockData[blockIdx] = new byte[blockLength];
				}
				if( blockOffsets[blockIdx] != segmentOffset ) {
					// Then it's a lost cause
					return;
				}
				if( segmentLength + segmentOffset > blockLength ) {
					logInvalidPacket("SEGMENT segment ("+segmentOffset+"+"+segmentLength+") extends past end of block ("+blockLength+")");
					return;
				}
				byte[] theBlockData = blockData[blockIdx];
				for( int i=0, destOffset=segmentOffset; i<segmentLength; ++i, ++destOffset, ++offset ) {
					theBlockData[destOffset] = data[offset];
				}
				blockOffsets[blockIdx] = segmentOffset+segmentLength;
				if( segmentOffset+segmentLength == blockLength ) {
					blockHandler.handlePacket(theBlockData, 0, blockLength);
				}
				break;
			}
			default:
				logInvalidPacket("Unrecognized opcode "+opcode+" at offset "+(offset-1));
				return;
			}
		}
	}
}
