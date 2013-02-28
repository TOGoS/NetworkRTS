package togos.networkrts.experimental.asyncjava;

public interface Puller<DataType>
{
	/**
	 * Must not be called by program segments.  Program segments may
	 * schedule a read by using {@link ProgramShell#onData(Puller, Trigger)}.
	 * If the end of a stream has been reached, this must return null.
	 */
	public DataType pull() throws Exception;
	/**
	 * Indicates to the puller that its output is no longer being used.
	 * Any current or future read()s should return null or throw an
	 * exception as soon as possible.
	 */
	public void stop();
}
