package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.Default;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushDefault implements OpcodeDefinition
{
	public static final PushDefault INSTANCE = new PushDefault();
	
	private PushDefault() { }

	@Override public String getUrn() { return "urn:sha1:5FPU2LGXPVNUR5J6UZN5KJTLJ5DDHXX2"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) {
		ds.pushStackItem(Default.INSTANCE);
		return offset+1;
	}
}
