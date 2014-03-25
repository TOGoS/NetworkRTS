package togos.networkrts.experimental.game19.io;

import togos.networkrts.util.HasURI;
import togos.networkrts.util.ResourceNotFound;

public interface WorldIO
{
	public HasURI saveObject( Object o );
	public Object getObject( HasURI ref ) throws ResourceNotFound;
	public <T> T getObject( HasURI ref, Class<T> expectedClass ) throws ResourceNotFound;
}
