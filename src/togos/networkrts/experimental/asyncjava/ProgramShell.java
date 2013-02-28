package togos.networkrts.experimental.asyncjava;

/**
 * The methods herein should be called *only* from the interpreter thread.
 * (i.e. the same thread that calls ProgramSegment#run)
 */
public interface ProgramShell
{
	public long getCurrentTime();
	/**
	 * Schedule a segment for immediate execution.
	 * Immediately scheduled segments should be run in reverse order that they are added
	 * and should all be run before any timed segments or I/O-triggered ones. 
	 */
	public void schedule( ProgramSegment seg );
	public void schedule( long timestamp, ProgramSegment seg );
	public <DataType> void onData( Puller<DataType> puller, Callback<DataType> trigger );
	public <DataType> void start( Puller<DataType> puller );
	public <DataType> void stop( Puller<DataType> puller );
}
