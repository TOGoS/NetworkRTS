package togos.networkrts.experimental.game18.demo;

import java.awt.Dimension;
import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.JFrame;

import togos.networkrts.experimental.game18.sim.InteractiveSimulationRunner;
import togos.networkrts.experimental.game18.sim.Room;
import togos.networkrts.experimental.game18.sim.Simulation;
import togos.networkrts.experimental.game18.sim.Message;
import togos.networkrts.experimental.qt2drender.demo.NetRenderDemo.RenderContext;
import togos.networkrts.experimental.qt2drender.demo.NetRenderDemo.VizStateCanvas;
import togos.networkrts.repo.BlobRepository;

public class Game18Demo
{
	float simulationSpeed = 10;
	
	ArrayBlockingQueue<Message> incomingMessageQueue = new ArrayBlockingQueue<Message>(32);
	Simulation sim = new Simulation();
	InteractiveSimulationRunner isr = new InteractiveSimulationRunner(incomingMessageQueue, sim, simulationSpeed);
	
	VizStateCanvas canv;
	long roomId = 1;
	
	public Game18Demo( VizStateCanvas canv ) {
		this.canv = canv;
	}
	
	public void updateDisplay() {
		Room r = sim.root.get(roomId, Room.class);
		
	}
	
	public void run() {
		isr.run();
	}
	
	public static void main( String[] args ) {
		BlobRepository blobRepo = new BlobRepository(new File(".ccouch"));
		final JFrame f = new JFrame("Game18Demo");
		final VizStateCanvas vsc = new VizStateCanvas(new RenderContext(blobRepo.toBlobResolver()));
		vsc.setPreferredSize(new Dimension(800,600));
		f.add(vsc);
		f.pack();
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		new Game18Demo(vsc).run();
	}
}
