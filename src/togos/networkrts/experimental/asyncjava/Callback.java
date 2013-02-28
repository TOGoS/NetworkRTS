package togos.networkrts.experimental.asyncjava;

public interface Callback<DataType>
{
	/**
	 * Will be called every time data is available,
	 * and called with null when the end of the stream has been reached.
	 */
	public ProgramSegment onData( Exception error, DataType data );
}
