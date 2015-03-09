package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushOpcodeByteValue implements OpcodeDefinition
{
	public static final PushOpcodeByteValue INSTANCE = new PushOpcodeByteValue();
	
	private PushOpcodeByteValue() { }
	
	@Override public String getUrn() { return "urn:sha1:7YDKP3YVPKA65E37OMZXWFCX3JZ2SM52"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) {
		ds.pushStackItem(data[offset]);
		return offset+1;
	}
}
