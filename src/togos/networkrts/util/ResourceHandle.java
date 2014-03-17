package togos.networkrts.util;

public interface ResourceHandle<T> extends HasURI
{
	public T getValueIfImmediatelyAvailable();
	public <X extends T> T getValue( Getter<X> populator ) throws ResourceNotFound;
}
