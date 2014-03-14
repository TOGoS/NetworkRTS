package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushInt8 implements OpcodeDefinition
{
	public static final PushInt8 INSTANCE = new PushInt8();
	
	private PushInt8() { }

	@Override public String getUrn() { return "urn:sha1:4L2WSIW5VU4GJIMI6VPMVMVYZTJAVBUJ"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) {
		ds.pushStackItem(data[offset+1]);
		return offset+2;
	}

}
