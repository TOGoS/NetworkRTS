package togos.networkrts.experimental.forthbots.lang;

public interface Handler<T,E extends Throwable>
{
	public void handle(T v) throws E;
}
