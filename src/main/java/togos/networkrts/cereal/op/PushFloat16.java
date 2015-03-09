package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.NumberEncoding;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushFloat16 implements OpcodeDefinition
{
	public static final PushFloat16 INSTANCE = new PushFloat16();
	
	private PushFloat16() { }

	@Override public String getUrn() { return "urn:sha1:VW3Q6OF4U7FNOLRQOKHCDBYFRERY4H3Q"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) {
		ds.pushStackItem(NumberEncoding.readFloat16(data,offset+1));
		return offset+3;
	}
}
