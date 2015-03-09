package togos.networkrts.experimental.asyncjava;

public interface ProgramSegment
{
	/**
	 * Must not block on I/O.
	 * Must not call other segments directly.
	 * May use all methods available on shell. 
	 */
	public void run( ProgramShell shell );
}
