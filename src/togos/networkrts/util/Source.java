package togos.networkrts.util;

public interface Source<T, E extends Throwable> {
	public T remove() throws E;
}
