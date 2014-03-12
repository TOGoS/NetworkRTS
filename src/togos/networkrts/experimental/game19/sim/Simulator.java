package togos.networkrts.experimental.game19.sim;

import java.util.concurrent.LinkedBlockingQueue;

import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.gensim.EventLooper;
import togos.networkrts.experimental.gensim.QueuelessRealTimeEventSource;

public class Simulator
{
	static class TaskRunner extends Thread {
		protected LinkedBlockingQueue<AsyncTask> incomingTasks;
		protected UpdateContext ctx;
		
		public TaskRunner( String name, LinkedBlockingQueue<AsyncTask> incomingTasks, UpdateContext ctx ) {
			super(name);
			this.incomingTasks = incomingTasks;
			this.ctx = ctx;
		}
		
		public void run() {
			while( !interrupted() ) {
				AsyncTask task;
				try {
					task = incomingTasks.take();
				} catch( InterruptedException e ) {
					System.err.println(getName()+" interrupted while waiting for incoming tasks");
					interrupt();
					return;
				}
				task.run( ctx );
			}
		}
	}
	
	protected QueuelessRealTimeEventSource<Message> incomingMessages = new QueuelessRealTimeEventSource<Message>();
	protected LinkedBlockingQueue<Message> outgoingMessages = new LinkedBlockingQueue<Message>();
	protected LinkedBlockingQueue<AsyncTask> asyncTaskQueue = new LinkedBlockingQueue<AsyncTask>();
	protected TaskRunner taskRunner = new TaskRunner("Async task runner", asyncTaskQueue, new UpdateContext() {
		@Override public void sendMessage( Message m ) {
			if( (m.minBitAddress & BitAddresses.TYPE_EXTERNAL) == BitAddresses.TYPE_EXTERNAL ) {
				outgoingMessages.add(m);
			}
			if( (m.maxBitAddress & BitAddresses.TYPE_EXTERNAL) == 0 ) {
				// Put back into incoming message queue!
				try {
					incomingMessages.post(m);
				} catch( InterruptedException e ) {
					System.err.println("Interrupted while posting message to incoming message queue from async task");
					e.printStackTrace();
					Thread.currentThread().interrupt();
				}
			}
		}
		@Override public void startAsyncTask( AsyncTask at ) {
			asyncTaskQueue.add(at);
		}
	});
	protected EventLooper<Message> looper;
	protected Simulation simulation;
	
	public Simulator( World world ) {
		simulation = new Simulation(world, asyncTaskQueue, outgoingMessages);
		looper = new EventLooper<Message>(incomingMessages, simulation, 10);
	}
	
	public void start() {
		taskRunner.start();
		looper.start();
	}
	
	public void halt() {
		taskRunner.interrupt();
		looper.interrupt();
	}
	
	public void enqueueMessage( Message m ) throws InterruptedException {
		incomingMessages.post(m);
	}
}
