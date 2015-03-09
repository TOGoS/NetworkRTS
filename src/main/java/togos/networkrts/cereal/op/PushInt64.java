package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.NumberEncoding;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushInt64 implements OpcodeDefinition
{
	public static final PushInt64 INSTANCE = new PushInt64();
	
	private PushInt64() { }

	@Override public String getUrn() { return "urn:sha1:EOVEY73MHEX7D6APBU2CPOOB6PJDCCFY"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) {
		ds.pushStackItem(NumberEncoding.readInt64(data,offset+1));
		return offset+9;
	}
}
