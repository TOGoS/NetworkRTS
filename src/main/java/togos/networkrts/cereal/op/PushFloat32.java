package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.NumberEncoding;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushFloat32 implements OpcodeDefinition
{
	public static final PushFloat32 INSTANCE = new PushFloat32();
	
	private PushFloat32() { }

	@Override public String getUrn() { return "urn:sha1:YL3EWK2DN4OTRHSUUY3MZYMRYMOSTGUE"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) {
		ds.pushStackItem(NumberEncoding.readFloat32(data,offset+1));
		return offset+5;
	}
}
