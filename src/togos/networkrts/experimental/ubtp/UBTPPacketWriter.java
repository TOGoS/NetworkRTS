package togos.networkrts.experimental.ubtp;

import java.util.zip.CRC32;

import togos.networkrts.cereal.NumberEncoding;
import togos.networkrts.experimental.tcp1.PacketHandler;

public class UBTPPacketWriter
{
	final byte[] buffer;
	int bufPos = 0;
	final PacketHandler forward;
	final CRC32 crcCalculator = new CRC32();
	
	public UBTPPacketWriter( int bufferSize, PacketHandler forward ) {
		this.buffer = new byte[bufferSize];
		this.forward = forward;
	}
	
	public void flush() {
		forward.handlePacket(buffer, 0, bufPos);
		bufPos = 0;
	}
	
	public void write( byte[] data, int off, int len ) {
		int wLen = len+3; // Total length of block+framing if written as a WHOLEBLOCK 
		
		// If it would fit in a single packet, but not in the current one, flush first.
		if( bufPos+wLen > buffer.length && wLen <= buffer.length ) flush();
		
		if( bufPos+wLen > buffer.length ) {
			crcCalculator.reset();
			crcCalculator.update(data, off, len);
			int crc = (int)crcCalculator.getValue();
			int hLen = 1+4+3+3+2;
			// TODO: Try harder to minimize number of packets
			if( bufPos+hLen >= buffer.length ) flush();
			
			int wrote = 0;
			while( wrote < len ) {
				int segLen = Math.min(wrote-len, buffer.length-bufPos-hLen);
				
				buffer[bufPos++] = UBTP.OP_SEGMENT;
				buffer[bufPos++] = (byte)(crc >> 24);
				buffer[bufPos++] = (byte)(crc >> 16);
				buffer[bufPos++] = (byte)(crc >>  8);
				buffer[bufPos++] = (byte)(crc >>  0);
				buffer[bufPos++] = (byte)(len >> 16);
				buffer[bufPos++] = (byte)(len >>  8);
				buffer[bufPos++] = (byte)(len >>  0);
				buffer[bufPos++] = (byte)(wrote >> 16);
				buffer[bufPos++] = (byte)(wrote >>  8);
				buffer[bufPos++] = (byte)(wrote >>  0);
				buffer[bufPos++] = (byte)(segLen >> 8);
				buffer[bufPos++] = (byte)(segLen >> 0);
				for( int i=0; i<segLen; ++i, ++wrote, ++bufPos ) {
					buffer[bufPos] = data[off+wrote];
				}
				flush();
			}
		} else {
			buffer[bufPos++] = UBTP.OP_WHOLEBLOCK;
			buffer[bufPos++] = (byte)(len >> 8);
			buffer[bufPos++] = (byte)(len >> 0);
			for( int i=0; i<len; ++i ) {
				buffer[bufPos++] = data[off+i];
			}
		}
		
		if( bufPos + 4 >= buffer.length ) flush();
	}
}
