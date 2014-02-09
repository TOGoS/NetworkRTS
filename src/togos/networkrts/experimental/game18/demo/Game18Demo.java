package togos.networkrts.experimental.game18.demo;

import java.awt.Dimension;
import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.JFrame;

import togos.networkrts.experimental.game18.sim.InteractiveSimulationRunner;
import togos.networkrts.experimental.game18.sim.Message;
import togos.networkrts.experimental.game18.sim.Room;
import togos.networkrts.experimental.game18.sim.Room.BoringestThingBehavior;
import togos.networkrts.experimental.game18.sim.Room.DynamicThing;
import togos.networkrts.experimental.game18.sim.Room.Neighbor;
import togos.networkrts.experimental.game18.sim.Room.Tile;
import togos.networkrts.experimental.game18.sim.Room.TileMapper;
import togos.networkrts.experimental.game18.sim.Simulation;
import togos.networkrts.experimental.game18.sim.IDUtil;
import togos.networkrts.experimental.qt2drender.VizState;
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
	
	public static void mainxxx( String[] args ) {
		BlobRepository blobRepo = new BlobRepository(new File(".ccouch"));
		final JFrame f = new JFrame("Game18Demo");
		final VizStateCanvas vsc = new VizStateCanvas(new RenderContext(blobRepo.toBlobGetter()));
		vsc.setPreferredSize(new Dimension(800,600));
		f.add(vsc);
		f.pack();
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		new Game18Demo(vsc).run();
	}
	
	public static void main( String[] args ) {
		Tile[] ta = new Tile(IDUtil.NO_ID, "urn:bitprint:EPNJUCXVUO2AVQN4I6OEJ5AVGVOUT3VP.W5JFWDVKZUM6YMMEX5OCZJZCDVONREJQ73O77GI", false, false, BoringestThingBehavior.<Tile>getInstance()).single;
		Tile[] tb = new Tile(IDUtil.NO_ID, "urn:bitprint:QI43S2L5OMDQEFVOMTMMH2IGAIXAFVC5.J4MDMEUBJM3H5D7QUNXXYGMVCSDBJ47ODAZPRZI", true , true , BoringestThingBehavior.<Tile>getInstance()).single;
		
		Room arr = new Room(1, 5, 5, 1, 0, 0, new Neighbor[0], null, new Tile[][] {
			tb, tb, tb, tb, tb,
			tb, ta, tb, tb, tb,
			tb, ta, ta, ta, tb,
			tb, ta, ta, tb, tb,
			tb, tb, tb, tb, tb
		}, new DynamicThing[0]);
		
		VizState vizState = Room.toVizState( new TileMapper(DemoUtil.DEFAULT_STORAGE_CONTEXT), arr, arr.id, 2.5f, 2.5f, 0.5f, 3 );
		
		DemoUtil.showVizStateWindow( vizState );
	}
}
