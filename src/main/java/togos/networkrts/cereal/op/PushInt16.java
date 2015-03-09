package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.NumberEncoding;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushInt16 implements OpcodeDefinition
{
	public static final PushInt16 INSTANCE = new PushInt16();
	
	private PushInt16() { }

	@Override public String getUrn() { return "urn:sha1:QSWZZUUD3T46FE7BMXAG54QLDJV336SJ"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) {
		ds.pushStackItem(NumberEncoding.readInt16(data,offset+1));
		return offset+3;
	}
}
