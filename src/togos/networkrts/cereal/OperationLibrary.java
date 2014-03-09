package togos.networkrts.cereal;

import java.util.List;

public class OperationLibrary
{
	public final String urn;
	public final byte[] sha1;
	public final List<OperationType> ops;
	
	public OperationLibrary( String urn, List<OperationType> ops ) {
		this.urn = urn;
		try {
			this.sha1 = CerealUtil.extractSha1FromUrn(urn);
		} catch( InvalidEncoding e ) { throw new RuntimeException(e); }
		this.ops = ops;
	}
}
