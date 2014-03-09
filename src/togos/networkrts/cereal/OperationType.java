package togos.networkrts.cereal;

import togos.networkrts.cereal.CerealDecoder.DecodeState;

interface OperationType
{
	/**
	 * Apply this operation, if applicable.
	 * 
	 * @param data the instruction stream
	 * @param offset the current instruction pointer 
	 * @param ds the decodestate
	 * @param context may be used to load external resources
	 * @return the new offset into the instruction stream.
	 *   This will be the same as the old offset to indicate that
	 *   this opcode did not apply.
	 * @throws InvalidEncoding 
	 */
	public int apply( byte[] data, int offset, DecodeState ds, CerealDecoder context ) throws InvalidEncoding;
}
