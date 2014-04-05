package togos.networkrts.experimental.game19.demo;

import java.io.IOException;

import togos.networkrts.experimental.game19.sim.Simulator;

public class Server
{
	protected final Simulator simulator;
	
	public Server( Simulator sim ) throws IOException {
		assert sim != null;
		
		this.simulator = sim;
	}
	
	public void start() {
		simulator.start();
	}
	
	public void setDaemon(boolean d) {
		simulator.setDaemon(d);
	}
}
