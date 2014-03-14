package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.CerealUtil;
import togos.networkrts.cereal.InvalidEncoding;
import togos.networkrts.cereal.NumberEncoding;
import togos.networkrts.cereal.OpcodeDefinition;

public class PushMediumString implements OpcodeDefinition
{
	public static final PushMediumString INSTANCE = new PushMediumString();
	
	private PushMediumString() { }

	@Override public String getUrn() { return "urn:sha1:GWALM6KZ47DHTPQJLI6RWYKPLAGG332D"; }
	
	@Override public int apply(byte[] data, int offset, DecodeState ds, CerealDecoder context) throws InvalidEncoding {
		int length = NumberEncoding.readInt16( data, offset+1 )&0xFFFF;
		ds.pushStackItem( CerealUtil.extract(data, offset+2, length) );
		return offset+3+length;
	}
}
