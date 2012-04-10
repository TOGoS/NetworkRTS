package togos.service;

import java.util.HashSet;
import java.util.Iterator;

import togos.service.Service;

/**
 * Aggregates multiple services so they can be treated
 * as one.
 */
public class ServiceManager implements Service
{
	protected boolean running;
	protected HashSet services = new HashSet();
	
	public synchronized void add(Service s) {
		services.add(s);
		if( running ) s.start();
	}
	
	public void remove(Service s) {
		synchronized( this ) {
			services.remove(s);
		}
		s.halt();
	}
	
	public synchronized void start() {
		if( running ) return;
		
		for( Iterator i=services.iterator(); i.hasNext(); ) {
			((Service)i.next()).start();
		}
		
		running = true;
	}
	
	public synchronized void halt() {
		if( !running ) return;
		
		for( Iterator i=services.iterator(); i.hasNext(); ) {
			((Service)i.next()).halt();
		}
		
		running = false;
	}
	
	public void join() {
		for( Iterator i=services.iterator(); i.hasNext(); ) {
			((Service)i.next()).join();
		}
	}
}
