package togos.networkrts.experimental.packet19;

import togos.blob.ByteChunk;

public class EthernetFrame extends BaseDataPacket
{
	protected long src, dest;
	protected ByteChunk payload;
	protected int tag; // Leave 0 for no tag
	protected short etherType;
	protected int crc;
	
	public EthernetFrame( long dest, long src, int tag, short etherType, ByteChunk payload ) {
		assert tag == 0 || (tag & 0x81000000) == 0x81000000; 
		
		this.objectPopulated = true;
		this.dest = dest;
		this.src = src;
		this.tag = tag;
		this.etherType = etherType;
		this.payload = payload;
	}
	
	public long getSourceAddress() {
		ensureObjectPopulated();
		return src;
	}
	
	public long getDestinationAddress() {
		ensureObjectPopulated();
		return src;
	}
}
