package togos.networkrts.cereal.op;

import togos.networkrts.cereal.CerealDecoder;
import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.cereal.CerealUtil;
import togos.networkrts.cereal.InvalidEncoding;
import togos.networkrts.cereal.OpcodeBehavior;
import togos.networkrts.cereal.OpcodeDefinition;
import togos.networkrts.util.ResourceNotFound;

public class LoadOpcode implements OpcodeDefinition
{
	public static final LoadOpcode INSTANCE = new LoadOpcode();
	
	private LoadOpcode() { }
	
	@Override public String getUrn() { return "urn:sha1:JKCUNETAZ6GE4FR4OA73NXX3QXZN3GYH"; }
	
	@Override public int apply( byte[] data, int offset, DecodeState ds, CerealDecoder context ) throws InvalidEncoding, ResourceNotFound {
		byte destOpId = data[offset+1];
		byte[] opSha1 = CerealUtil.extract(data, offset+2, 20);
		String opSha1Urn = CerealUtil.sha1Urn(opSha1);
		OpcodeBehavior opBehavior = context.opLib.get(opSha1Urn);
		if( opBehavior == null ) throw new ResourceNotFound(opSha1Urn);
		ds.setOpcodeBehavior(destOpId, opBehavior);
		return offset+22;
	}
}
