package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.NumberEncoding;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushFloat64 implements OpcodeDefinition
{
	public static final PushFloat64 INSTANCE = new PushFloat64();
	
	private PushFloat64() { }

	@Override public String getUrn() { return "urn:sha1:3A4MH5LSPZILHYE6FJQJVTFNCTIC2LPW"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) {
		ds.pushStackItem(NumberEncoding.readFloat64(data,offset+1));
		return offset+9;
	}
}
