package togos.networkrts.util;

public interface ResourceHandle<T>
{
	public String getUri();
	public T getValue( Getter<T> populator ) throws ResourceNotFound;
}
