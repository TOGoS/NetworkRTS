package togos.networkrts.experimental.gensim;

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
		
		t1 = eq.currentTimestamp;
		eq.add(new TestEvent(t1, "Matthew"));
		evt = eq.take();
		assertEquals( "Matthew", evt.getPayload() );
		t2 = eq.currentTimestamp;
		
		assertEquals( 0, (t2-t1)/10 );
	}
	
	public void testSomeDelay() throws InterruptedException {
		long t1, t2;
		TestEvent evt;
		TimedEventQueue<TestEvent> eq = new TimedEventQueue<TestEvent>();
		
		t1 = eq.currentTimestamp;
		eq.add(new TestEvent(t1+50, "Matthew"));
		assertNull( eq.peek() );
		eq.advanceTimeTo( t1 + 25 );
		assertNull( eq.peek() );
		eq.advanceTimeTo( t1 + 50 );
		assertNotNull( eq.peek() );
		evt = eq.take();
		assertEquals( "Matthew", evt.getPayload() );
		t2 = eq.currentTimestamp;
		
		assertEquals( 5, (t2-t1)/10 );
	}
	
	public void testInsert3Things() throws InterruptedException {
		long t1, t2;
		TestEvent evt;
		TimedEventQueue<TestEvent> eq = new TimedEventQueue<TestEvent>();
		
		t1 = eq.currentTimestamp;
		eq.add(new TestEvent(t1, "Jon"));
		eq.add(new TestEvent(t1+50, "Matthew"));
		eq.add(new TestEvent(t1+20, "Luke"));
		
		assertNotNull( eq.peek() );
		evt = eq.take();
		assertEquals( "Jon", evt.getPayload() );
		t2 = eq.currentTimestamp;
		assertEquals( 0, (t2-t1)/10 );
		
		eq.advanceTimeTo( t1+20 );
		assertNotNull( eq.peek() );
		evt = eq.take();
		assertEquals( "Luke", evt.getPayload() );
		t2 = eq.currentTimestamp;
		assertEquals( 2, (t2-t1)/10 );
		
		eq.advanceTimeTo( t1+50 );
		assertNotNull( eq.peek() );
		evt = eq.take();
		assertEquals( "Matthew", evt.getPayload() );
		t2 = eq.currentTimestamp;
		assertEquals( 5, (t2-t1)/10 );
	}
	
	public void testInsertWhileWaiting() throws InterruptedException {
		final TimedEventQueue<TestEvent> eq = new TimedEventQueue<TestEvent>();
		final long t1 = eq.currentTimestamp;
		
		eq.add( new TestEvent(t1+90,"Greg") );
		
		Thread t = new Thread() {
			@Override
			public void run() {
				TestEvent evt;
				long t2;
				
				try {
					evt = eq.take();
					assertEquals( "Jon", evt.getPayload() );
					t2 = eq.currentTimestamp;
					assertEquals( 2, (t2-t1)/10 );
					
					evt = eq.take();
					assertEquals( "Luke", evt.getPayload() );
					t2 = eq.currentTimestamp;
					assertEquals( 5, (t2-t1)/10 );
					
					evt = eq.take();
					assertEquals( "Greg", evt.getPayload() );
					t2 = eq.currentTimestamp;
					assertEquals( 9, (t2-t1)/10 );
				} catch( InterruptedException e ) {
					throw new RuntimeException(e);
				}
			}
		};
		t.start();
		
		eq.add( new TestEvent(t1+50,"Luke") );
		eq.add( new TestEvent(t1+20,"Jon") );
		Thread.sleep(1);
		eq.advanceTimeTo(t1+20);
		Thread.sleep(1);
		eq.advanceTimeTo(t1+50);
		Thread.sleep(1);
		eq.advanceTimeTo(t1+90);
		
		t.join();
	}
}
