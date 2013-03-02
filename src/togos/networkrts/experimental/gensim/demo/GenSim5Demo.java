package togos.networkrts.experimental.gensim.demo;

import java.util.ArrayList;
import java.util.PriorityQueue;

import togos.networkrts.experimental.gensim.EventLoop;
import togos.networkrts.experimental.gensim.QueuelessRealTimeEventSource;
import togos.networkrts.experimental.gensim.Stepper;
import togos.networkrts.util.Timed;

public class GenSim5Demo
{	
	static class DemoActor {
		DemoSimulation w;
		String name;
		double x, y;
		public DemoActor( DemoSimulation w, String name, double x, double y ) {
			this.w = w;
			this.name = name;
			this.x = x; this.y = y;
		}
		public void messageReceived( String message ) {
			if( message.length() < 100 ) {
				String m = name+" overheard: "+message;
				System.out.println(m);
				w.transmit( x, y, m );
			}
		}
	}
	
	interface DemoEvent {
		public void run( DemoSimulation s );
	}
		
	static class Transmission implements DemoEvent {
		double sourceX, sourceY;
		String message;
		public Transmission( double x, double y, String m ) {
			this.sourceX = x;
			this.sourceY = y;
			this.message = m;
		}
		public void run( DemoSimulation w ) {
			w.transmit( sourceX, sourceY, message );
		}
	}
	
	static class Reception implements DemoEvent {
		DemoActor receiver;
		String message;
		public Reception( DemoActor receiver, String message ) {
			this.receiver = receiver;
			this.message = message;
		}
		public void run( DemoSimulation w ) {
			receiver.messageReceived( this.message );
		}
	}
	
	static class DemoSimulation implements Stepper<DemoEvent> {
		PriorityQueue<Timed<DemoEvent>> timerQueue = new PriorityQueue();
		ArrayList<DemoActor> actors = new ArrayList();
		long currentTime = 0;
		
		public void enqueueEvent( long targetTime, DemoEvent evt ) {
			timerQueue.add( new Timed(targetTime, 0, evt) );
		}
		
		public long getNextInternalUpdateTime() {
			Timed timer = timerQueue.peek();
			return timer == null ? Long.MAX_VALUE : timer.time;
		}
		
		public void setCurrentTime( long t ) {
			currentTime = t;
			Timed<DemoEvent> timer;
			while( (timer = timerQueue.peek()) != null && timer.time <= currentTime ) {
				timerQueue.remove();
				timer.payload.run(this);
			}
		}
		
		public void transmit( double sourceX, double sourceY, String message ) {
			for( DemoActor actor : actors ) {
				double dx = actor.x-sourceX, dy = actor.y-sourceY;
				double dist = Math.sqrt(dx*dx+dy*dy);
				if( dist == 0 ) continue;
				timerQueue.add( new Timed(currentTime+(long)(dist*100), 0, new Reception(actor, message)));
			}
		}
		
		@Override public void handleEvent( DemoEvent evt ) {
			evt.run(this);
		}
	}
	
	public static void main( String[] args ) throws Exception {
		final DemoSimulation sim = new DemoSimulation();
		sim.actors.add( new DemoActor( sim, "Frank",  1,  1 ));
		sim.actors.add( new DemoActor( sim, "Ralph", -1, -1 ));
		final QueuelessRealTimeEventSource<DemoEvent> es = new QueuelessRealTimeEventSource();
		es.post( new Transmission(0, 0, "Yuk yuk!") );
		EventLoop.run( es, sim );
	}
}
