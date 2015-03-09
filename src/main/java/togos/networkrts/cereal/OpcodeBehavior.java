package togos.networkrts.cereal;

import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.util.ResourceNotFound;

public interface OpcodeBehavior
{
	/**
	 * Apply this operation, if applicable.
	 * 
	 * @param data the instruction stream
	 * @param offset the current instruction pointer 
	 * @param ds the decodestate
	 * @param context may be used to load external resources
	 * @return the new offset into the instruction stream.
	 * @throws InvalidEncoding 
	 */
	public int apply( byte[] data, int offset, DecodeState ds, CerealDecoder context ) throws InvalidEncoding, ResourceNotFound;
}
