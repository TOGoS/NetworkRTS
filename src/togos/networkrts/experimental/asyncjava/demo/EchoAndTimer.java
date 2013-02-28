package togos.networkrts.experimental.asyncjava.demo;

import java.io.IOException;

import togos.networkrts.experimental.asyncjava.ProgramSegment;
import togos.networkrts.experimental.asyncjava.ProgramShell;
import togos.networkrts.experimental.asyncjava.Callback;
import togos.networkrts.experimental.asyncjava.interp.ProgramRunner;
import togos.networkrts.experimental.asyncjava.io.InputStreamPuller;

/**
 * Demonstrates using the asyncjava event loop
 * to run a program that reads from standard input
 * and execute a thing every so many milliseconds.
 */
public class EchoAndTimer
{
	static int exitCode = 0;
	
	public static void main( String[] args ) {
		ProgramRunner pr = new ProgramRunner();
		pr.schedule( new ProgramSegment() {
			boolean quit = false;
			
			@Override public void run(ProgramShell shell) {
				long startTime = shell.getCurrentTime();
				System.err.println("I am starting.  Time = "+startTime);
				final InputStreamPuller stdinPuller = new InputStreamPuller(System.in);
				shell.onData( stdinPuller, new Callback<byte[]>() {
					@Override public ProgramSegment onData(final Exception error, final byte[] data) {
						return new ProgramSegment() {
							@Override public void run(ProgramShell shell) {
								if( data == null ) {
									System.err.println("Read end of stream");
									return;
								}
								if( error != null ) {
									System.err.println("Error reading: "+error);
								} else {
									System.err.println("Read "+data.length+" bytes");
								}
								if( data[0] == 'e' ) {
									System.err.println("Quitting with exit code = 1!");
									shell.stop(stdinPuller);
									exitCode = 1;
								}
								if( data[0] == 'q' ) {
									System.err.println("Quitting!");
									shell.stop(stdinPuller);
									exitCode = 0;
								}
								try {
									System.out.write(data);
								} catch (IOException e) {
									System.err.println("Error writing to STDOUT: "+e);
									e.printStackTrace();
								}
							}
						};
					}
				});
				shell.schedule( startTime, new ProgramSegment() {
					long nextTime = System.currentTimeMillis(); 
					@Override public void run(ProgramShell shell) {
						long curTime = nextTime;
						nextTime += 1000;
						if( !quit ) shell.schedule( nextTime, this );
						System.err.println("Tick! " + curTime + " (actual = " + shell.getCurrentTime() + ")" );
					}
				});
				shell.start(stdinPuller);
			}
		});
		pr.run();
		// This is necessary because InputStream sometimes blocks
		// if #close is called during a call to #read.
		System.exit(exitCode);
	}
}
