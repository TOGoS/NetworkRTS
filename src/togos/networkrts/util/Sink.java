package togos.networkrts.util;

public interface Sink<T, E extends Throwable> {
	public void add(T t) throws E;
}
