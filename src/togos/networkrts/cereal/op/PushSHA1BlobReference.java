package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.CerealUtil;
import togos.networkrts.cereal.InvalidEncoding;
import togos.networkrts.cereal.OpcodeDefinition;
import togos.networkrts.cereal.SHA1ObjectReference;

public class PushSHA1BlobReference implements OpcodeDefinition
{
	public static final PushSHA1BlobReference INSTANCE = new PushSHA1BlobReference();
	
	private PushSHA1BlobReference() { }

	@Override public String getUrn() { return "urn:sha1:FMREHWOVMKFUSN5QFXHJZUOVOCX6WI4P"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) throws InvalidEncoding {
		ds.pushStackItem(new SHA1ObjectReference( CerealUtil.extract(data, offset+1, 20), false ));
		return offset+21;
	}
}
