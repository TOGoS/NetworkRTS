package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.CerealUtil;
import togos.networkrts.cereal.InvalidEncoding;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushShortString implements OpcodeDefinition
{
	public static final PushShortString INSTANCE = new PushShortString();
	
	private PushShortString() { }

	@Override public String getUrn() { return "urn:sha1:BAJS62OILFSDNYS2VZNP6BWLWRUQYNHP"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) throws InvalidEncoding {
		int length = data[offset+1]&0xFF;
		ds.pushStackItem( CerealUtil.extract(data, offset+2, length) );
		return offset+2+length;
	}
}
