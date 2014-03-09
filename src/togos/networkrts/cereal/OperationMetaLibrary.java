package togos.networkrts.cereal;

import java.util.Collections;
import java.util.HashMap;

import togos.networkrts.cereal.CerealDecoder.DecodeState;
import togos.networkrts.util.ResourceNotFound;

class OperationMetaLibrary {
	protected HashMap<String, OperationLibrary> libs = new HashMap<String, OperationLibrary>();
	
	public final OperationType loadLib = new OperationType() {
		@Override public int apply( byte[] data, int offset, DecodeState ds, CerealDecoder context ) throws InvalidEncoding, ResourceNotFound {
			if( data[offset] == 0x01 ) {
				byte[] libSha1 = CerealUtil.extract(data, offset+1, 20);
				String libUrn = CerealUtil.sha1Urn(libSha1);
				OperationLibrary lib = libs.get(libUrn);
				if( lib == null ) throw new ResourceNotFound(libUrn);
				ds.addOperationTypes( lib.ops );
				return offset+21;
			}
			return offset;
		}
	};
	
	public void addLibrary( OperationLibrary lib ) {
		libs.put( CerealUtil.sha1Urn(lib.sha1), lib );
	}
	
	protected DecodeState getInitialDecodeState() {
		return new DecodeState(Collections.<OperationType>singletonList(loadLib));
	}
}