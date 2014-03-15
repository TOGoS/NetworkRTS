package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.CerealUtil;
import togos.networkrts.cereal.InvalidEncoding;
import togos.networkrts.cereal.OpcodeDefinition;
import togos.networkrts.cereal.SHA1ObjectReference;

public class PushSHA1ObjectReference implements OpcodeDefinition
{
	public static final PushSHA1ObjectReference INSTANCE = new PushSHA1ObjectReference();
	
	private PushSHA1ObjectReference() { }

	@Override public String getUrn() { return "urn:sha1:OH3EHU6FYXJNXXV2QUJTYIUEAYC57JEM"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) throws InvalidEncoding {
		ds.pushStackItem(new SHA1ObjectReference( CerealUtil.extract(data, offset+1, 20), true ));
		return offset+21;
	}
}
