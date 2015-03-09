package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushTrue implements OpcodeDefinition
{
	public static final PushTrue INSTANCE = new PushTrue();
	
	private PushTrue() { }

	@Override public String getUrn() { return "urn:sha1:7MCH6UFK4BOESVRPTLVBE6DI2K3DMZWE"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) {
		ds.pushStackItem(Boolean.TRUE);
		return offset+1;
	}
}
