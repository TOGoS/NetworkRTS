package togos.networkrts.experimental.game19.io;

import java.io.IOException;
import java.io.OutputStream;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.InvalidEncoding;
import togos.networkrts.util.ResourceNotFound;

public interface WorldObjectCCCodec<T>
{
	public Class<T> getEncodableClass();
	
	/**
	 * Write the object to the stream.
	 * constructorPrefix is the constructor opcode+constructor number
	 * that maps back to this codec.
	 */
	public void encode(
		T obj, byte[] constructorPrefix, OutputStream os, CerealWorldIO cwio
	) throws IOException;
	
	/**
	 * This is distinct from OpcodeBehavior in that
	 * offset will be the offset to any custom data,
	 * not to the opcode (which presumably is somewhere before offset)
	 */
	public int decode(
		byte[] data, int offset, DecodeState ds, CerealDecoder context
	) throws InvalidEncoding, ResourceNotFound;
}
