package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.NumberEncoding;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushInt32 implements OpcodeDefinition
{
	public static final PushInt32 INSTANCE = new PushInt32();
	
	private PushInt32() { }

	@Override public String getUrn() { return "urn:sha1:SQ5BXF65BH4QSTEBK65HSJCC6T2EB4TK"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) {
		ds.pushStackItem(NumberEncoding.readInt32(data,offset+1));
		return offset+5;
	}
}
