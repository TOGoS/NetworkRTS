package togos.networkrts.experimental.tcp1;

import togos.networkrts.util.ByteUtil;

public abstract class EthernetFrameHandler implements PacketHandler
{
	// Ethernet packets coming through TAP have the following format:
	//  0... 6  destination address
	//  6...12  source address
	// 12...14  encapsulated protocol number (0x0800 = IPv4)
	// 14...    encapsulated packet
	
	@Override public void handlePacket(byte[] data, int offset, int length) {
		if( length < 14 ) {
			handleInvalidPacket(data, offset, length);
		} else {
			handlePacket(
				ByteUtil.decodeUInt48(data, offset),
				ByteUtil.decodeUInt48(data, offset+6),
				ByteUtil.decodeUInt16(data, offset+12),
				data, offset + 14, length - 14
			);
		}
	}
	
	protected void handleInvalidPacket(byte[] data, int offset, int length) { }
	protected abstract void handlePacket(long destMac, long sourceMac, int protocol, byte[] data, int payloadOffset, int payloadLength);
}
