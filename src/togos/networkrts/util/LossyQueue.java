package togos.networkrts.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Queue-like object where multiple consecutive adds/puts overwrite a single value.
 */
public class LossyQueue<T> implements BlockingQueue<T>
{
	protected volatile T item = null;
	
	//// Non-destructive queries
	
	@Override public T element() {
		T i = item;
		if( i == null ) throw new NoSuchElementException();
		return i;
	}
	
	@Override public T peek() {
		return item;
	}
	
	@Override public boolean containsAll(Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}
	
	@Override public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override public Iterator<T> iterator() {
		throw new UnsupportedOperationException();
	}
	
	@Override public synchronized int size() {
		return item == null ? 0 : 1;
	}
	
	@Override public Object[] toArray() {
		throw new UnsupportedOperationException();
	}
	
	@Override public <Q> Q[] toArray(Q[] is) {
		throw new UnsupportedOperationException();
	}
	
	@Override public boolean contains(Object arg0) {
		throw new UnsupportedOperationException();
	}
	
	@Override public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}
	
	//// Destructive queries
	
	@Override public synchronized T poll() {
		T i = item;
		item = null;
		return i;
	}
	
	@Override public synchronized T remove() {
		T i = item;
		item = null;
		if( i == null ) throw new NoSuchElementException();
		return i;
	}
	
	@Override public synchronized T poll(long timeout, TimeUnit tu) throws InterruptedException {
		long endBy = System.currentTimeMillis() + tu.toMillis(timeout);
		long ctime;
		while( endBy < (ctime = System.currentTimeMillis()) && item == null ) {
			wait(ctime - endBy);
		}
		T i = item;
		item = null;
		return i;
	}

	@Override public synchronized boolean remove(Object i) {
		if( i.equals(item) ) {
			item = null;
			return true;
		} else return false;
	}

	@Override public synchronized T take() throws InterruptedException {
		while( item == null ) wait();
		T i = item;
		item = null;
		return i;
	}

	@Override public int drainTo(Collection<? super T> arg0) {
		throw new UnsupportedOperationException();
	}

	@Override public int drainTo(Collection<? super T> arg0, int arg1) {
		throw new UnsupportedOperationException();
	}
	
	@Override public synchronized void clear() {
		item = null;
	}
	
	@Override public boolean removeAll(Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}

	@Override public boolean retainAll(Collection<?> arg0) {
		throw new UnsupportedOperationException();
	}
	
	//// Add (need to notify if adding a non-null)
	
	@Override public boolean add(T i) {
		if( i == null ) return false;
		
		synchronized(this) {
			item = i;
			notify();
		}
		
		return true;
	}
	
	@Override public void put(T i) throws InterruptedException {
		add(i);
	}
	
	@Override public boolean offer(T i) {
		return add(i);
	}
	
	@Override public boolean offer(T i, long timeout, TimeUnit unit) throws InterruptedException {
		return add(i);
	}
	
	@Override public boolean addAll(Collection<? extends T> collection) {
		T i = null;
		for( T i1 : collection ) i = i1;
		return add(i);
	}
}
