package togos.networkrts.util;

import java.util.HashMap;

import togos.networkrts.repo.BitprintDigest;

public class MemoryRepo implements Getter<byte[]>, Storer<byte[]>
{
	protected final HashMap<String,byte[]> store = new HashMap<String,byte[]>();
	
	public String store( byte[] data ) {
		BitprintDigest digest = new BitprintDigest();
		digest.update(data);
		String urn = BitprintDigest.toUrn(digest.digest());
		store.put(urn, data);
		return urn;
	}

	@Override public byte[] get( String uri ) throws ResourceNotFound {
		byte[] data = store.get(uri);
		if( data == null ) throw new ResourceNotFound(uri);
		return data;
	}
}
