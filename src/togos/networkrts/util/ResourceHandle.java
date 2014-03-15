package togos.networkrts.util;

public interface ResourceHandle<T> extends HasURI
{
	public <X extends T> T getValue( Getter<X> populator ) throws ResourceNotFound;
}
