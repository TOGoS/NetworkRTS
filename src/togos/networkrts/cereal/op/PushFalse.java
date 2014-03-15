package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushFalse implements OpcodeDefinition
{
	public static final PushFalse INSTANCE = new PushFalse();
	
	private PushFalse() { }

	@Override public String getUrn() { return "urn:sha1:QMLAOM5KF6GSGLWPKQ7DQEBP47HYCVAM"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) {
		ds.pushStackItem(Boolean.FALSE);
		return offset+1;
	}
}
