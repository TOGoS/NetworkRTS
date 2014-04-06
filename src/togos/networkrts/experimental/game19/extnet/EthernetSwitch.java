package togos.networkrts.experimental.game19.extnet;

import java.util.HashMap;
import java.util.Map;

import togos.networkrts.experimental.game19.util.MessageSender;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.packet19.EthernetFrame;
import togos.networkrts.util.BitAddressUtil;

interface EthernetFrameSender {
	public void send( EthernetFrame f );
}

public class EthernetSwitch implements MessageSender {
	protected final long bitAddress;
	protected final MessageSender sender;
	
	// Map ethernet addresses to bit addresses
	// TODO: Use a more efficient table implementation
	protected final Map<Long,Long> etherToBit = new HashMap<Long,Long>();
	
	public EthernetSwitch( long bitAddress, MessageSender sender ) {
		this.bitAddress = bitAddress;
		this.sender = sender;
	}
	
	public void sendMessage(Message m) {
		if( !BitAddressUtil.rangeContains(m, bitAddress) ) return;
		
		Object p = m.payload;
		if( p instanceof EthernetFrame ) {
			EthernetFrame f = (EthernetFrame)p;
			if( m.sourceAddress != BitAddressUtil.NO_ADDRESS ) {
				Long sourceAddy = f.getSourceAddress();
				etherToBit.put(sourceAddy, m.sourceAddress);
			}
			Long destAddy = f.getDestinationAddress();
			Long destBa = etherToBit.get(destAddy);
			
			sender.sendMessage(
				destBa == null ?
					Message.create(BitAddressUtil.MIN_ADDRESS, BitAddressUtil.MAX_ADDRESS, Message.MessageType.INCOMING_PACKET, bitAddress, f ) :
					Message.create(destBa, destBa, Message.MessageType.INCOMING_PACKET, bitAddress, f )
			);
		} else {
			System.err.println("External switch received something other than an ethernet frame :X");
		}
	}
}
