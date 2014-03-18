package togos.networkrts.experimental.gensim;

import junit.framework.TestCase;
import togos.networkrts.util.Timer;

public class TimedEventQueueTest extends TestCase
{
	public void testNoDelay() throws InterruptedException {
		long t1, t2;
		Timer evt;
		TimedEventQueue<String> eq = new TimedEventQueue<String>();
		
		t1 = System.currentTimeMillis();
		eq.enqueue(t1, "Matthew");
		evt = eq.take();
		assertEquals( "Matthew", evt.payload );
		t2 = System.currentTimeMillis();
		
		assertEquals( 0, (t2-t1)/10 );
	}
	
	public void testSomeDelay() throws InterruptedException {
		long t1, t2;
		Timer evt;
		TimedEventQueue<String> eq = new TimedEventQueue<String>();
		
		t1 = System.currentTimeMillis();
		eq.enqueue(t1+50, "Matthew");
		assertNull( eq.peek() );
		assertNull( eq.peek() );
		Thread.sleep(50);
		assertNotNull( eq.peek() );
		evt = eq.take();
		assertEquals( "Matthew", evt.payload );
		t2 = System.currentTimeMillis();
		
		assertEquals( 5, (t2-t1)/10 );
	}
	
	public void testInsert3Things() throws InterruptedException {
		long t1, t2;
		Timer evt;
		TimedEventQueue<String> eq = new TimedEventQueue<String>();
		
		t1 = System.currentTimeMillis();
		eq.enqueue(t1, "Jon");
		eq.enqueue(t1+50, "Matthew");
		eq.enqueue(t1+20, "Luke");
		
		assertNotNull( eq.peek() );
		evt = eq.take();
		assertEquals( "Jon", evt.payload );
		t2 = System.currentTimeMillis();
		assertEquals( 0, (t2-t1)/10 );
		
		Thread.sleep(20);
		assertNotNull( eq.peek() );
		evt = eq.take();
		assertEquals( "Luke", evt.payload );
		t2 = System.currentTimeMillis();
		assertEquals( 2, (t2-t1)/10 );
		
		Thread.sleep(30);
		assertNotNull( eq.peek() );
		evt = eq.take();
		assertEquals( "Matthew", evt.payload );
		t2 = System.currentTimeMillis();
		assertEquals( 5, (t2-t1)/10 );
	}
	
	public void testInsertWhileWaiting() throws InterruptedException {
		final TimedEventQueue<String> eq = new TimedEventQueue<String>();
		final long t1 = System.currentTimeMillis();
		
		eq.enqueue( t1+90,"Greg" );
		
		Thread t = new Thread() {
			@Override
			public void run() {
				Timer evt;
				long t2;
				
				try {
					evt = eq.take();
					assertEquals( "Jon", evt.payload );
					t2 = System.currentTimeMillis();
					assertEquals( 2, (t2-t1)/10 );
					
					evt = eq.take();
					assertEquals( "Luke", evt.payload );
					t2 = System.currentTimeMillis();
					assertEquals( 5, (t2-t1)/10 );
					
					evt = eq.take();
					assertEquals( "Greg", evt.payload );
					t2 = System.currentTimeMillis();
					assertEquals( 9, (t2-t1)/10 );
				} catch( InterruptedException e ) {
					throw new RuntimeException(e);
				}
			}
		};
		t.start();
		
		eq.enqueue( t1+50,"Luke" );
		eq.enqueue( t1+20,"Jon" );
		Thread.sleep(1);
		Thread.sleep(1);
		Thread.sleep(1);
		
		t.join();
	}
}
