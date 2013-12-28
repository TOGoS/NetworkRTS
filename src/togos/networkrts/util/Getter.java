package togos.networkrts.util;

public interface Getter<T> {
	public T get(String uri) throws Exception;
}