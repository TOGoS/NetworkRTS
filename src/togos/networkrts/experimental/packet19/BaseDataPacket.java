package togos.networkrts.experimental.packet19;

/**
 * Base class for objects representing packets that can lazily convert
 * between their serialized and interpreted forms.
 * 
 * Subclasses should implement populateData and populateObject.
 */
public abstract class BaseDataPacket implements DataPacket
{
	protected boolean dataPopulated, objectPopulated;
	protected byte[] data;
	protected int dataOffset, dataSize;
	
	protected BaseDataPacket() { }
	
	protected BaseDataPacket( byte[] data, int offset, int size ) {
		this.dataPopulated = true;
		this.dataOffset = offset;
		this.dataSize = size;
		this.data = data;
	}
	
	protected void populateData() {
		throw new UnsupportedOperationException();
	}
	
	protected void ensureDataPopulated() {
		if( !dataPopulated ) populateData();
		dataPopulated = true;
	}
	
	@Override public byte[] getBuffer() {
		ensureDataPopulated();
		return data;
	}
	
	@Override public int getOffset() {
		ensureDataPopulated();
		return dataOffset;
	}
	
	@Override public int getSize() {
		ensureDataPopulated();
		return dataSize;
	}
	
	protected void populateObject() {
		throw new UnsupportedOperationException();
	}
	
	protected void ensureObjectPopulated() {
		if( !objectPopulated ) populateObject();
		objectPopulated = true;
	}
}
