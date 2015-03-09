package togos.service;

public abstract class InterruptableSingleThreadedService implements Runnable, Service
{
	Thread thread;
	
	protected abstract void _run() throws InterruptedException;
	
	protected String getServiceThreadName() {
		return getClass().getName();
	}
	
	public final void run() {
		try {
			_run();
		} catch( InterruptedException e ) {
			Thread.currentThread().interrupt();
		} finally {
			synchronized( this ) {
				thread = null;
			}
		}
	}
	
	public synchronized void start() {
		if( thread != null ) return;
		
		thread = new Thread(this, getServiceThreadName());
		thread.start();
	}
	
	public void halt() {
		Thread t = thread;
		if( t != null ) t.interrupt(); 
	}
	
	public void join() {
		Thread t = thread;
		if( t != null ) try {
			t.join();
		} catch( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}
}
