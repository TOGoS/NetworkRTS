package togos.networkrts.experimental.netsim1;

import junit.framework.TestCase;

public class TimedEventQueueTest extends TestCase
{
	class TestEvent implements Timestamped
	{
		final long ts;
		final String payload;
		
		public TestEvent( long ts, String payload ) {
			this.ts = ts;
			this.payload = payload;
		}
		
		public long getTimestamp() {  return ts;  }
		public String getPayload() {  return payload;  }
	}
	
	public void testNoDelay() throws InterruptedException {
		long t1, t2;
		TestEvent evt;
		TimedEventQueue<TestEvent> eq = new TimedEventQueue<TestEvent>();
		
		t1 = System.currentTimeMillis();
		eq.add(new TestEvent(t1, "Matthew"));
		evt = eq.take();
		assertEquals( "Matthew", evt.getPayload() );
		t2 = System.currentTimeMillis();
		
		assertEquals( 0, (t2-t1)/10 );
	}
	
	public void testSomeDelay() throws InterruptedException {
		long t1, t2;
		TestEvent evt;
		TimedEventQueue<TestEvent> eq = new TimedEventQueue<TestEvent>();
		
		t1 = System.currentTimeMillis();
		eq.add(new TestEvent(t1+50, "Matthew"));
		evt = eq.take();
		assertEquals( "Matthew", evt.getPayload() );
		t2 = System.currentTimeMillis();
		
		assertEquals( 5, (t2-t1)/10 );
	}
	
	public void testInsert3Things() throws InterruptedException {
		long t1, t2;
		TestEvent evt;
		TimedEventQueue<TestEvent> eq = new TimedEventQueue<TestEvent>();
		
		t1 = System.currentTimeMillis();
		eq.add(new TestEvent(t1, "Jon"));
		eq.add(new TestEvent(t1+50, "Matthew"));
		eq.add(new TestEvent(t1+20, "Luke"));
		
		evt = eq.take();
		assertEquals( "Jon", evt.getPayload() );
		t2 = System.currentTimeMillis();
		assertEquals( 0, (t2-t1)/10 );
		
		evt = eq.take();
		assertEquals( "Luke", evt.getPayload() );
		t2 = System.currentTimeMillis();
		assertEquals( 2, (t2-t1)/10 );
		
		evt = eq.take();
		assertEquals( "Matthew", evt.getPayload() );
		t2 = System.currentTimeMillis();
		assertEquals( 5, (t2-t1)/10 );
	}
	
	public void testInsertWhileWaiting() throws InterruptedException {
		final TimedEventQueue<TestEvent> eq = new TimedEventQueue<TestEvent>();
		final long t1 = System.currentTimeMillis();
		
		eq.add( new TestEvent(t1+90,"Greg") );
		
		Thread t = new Thread() {
			@Override
			public void run() {
				TestEvent evt;
				long t2;
				
				try {
					evt = eq.take();
					assertEquals( "Jon", evt.getPayload() );
					t2 = System.currentTimeMillis();
					assertEquals( 2, (t2-t1)/10 );
					
					evt = eq.take();
					assertEquals( "Luke", evt.getPayload() );
					t2 = System.currentTimeMillis();
					assertEquals( 5, (t2-t1)/10 );

					evt = eq.take();
					assertEquals( "Greg", evt.getPayload() );
					t2 = System.currentTimeMillis();
					assertEquals( 9, (t2-t1)/10 );
				} catch( InterruptedException e ) {
					throw new RuntimeException(e);
				}
			}
		};
		t.start();
		
		Thread.sleep(5);
		eq.add( new TestEvent(t1+50,"Luke") );
		Thread.sleep(5);
		eq.add( new TestEvent(t1+20,"Jon") );
		
		t.join();
	}
}
