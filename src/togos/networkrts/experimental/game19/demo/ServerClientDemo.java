package togos.networkrts.experimental.game19.demo;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import togos.networkrts.experimental.game19.Renderer;
import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.scene.LayerData;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.WorldNode;
import togos.networkrts.experimental.game19.world.beh.NoBehavior;
import togos.networkrts.experimental.game19.world.beh.RandomWalkBehavior;
import togos.networkrts.experimental.game19.world.encoding.WorldConverter;
import togos.networkrts.experimental.game19.world.gen.SolidNodeFiller;
import togos.networkrts.experimental.game19.world.gen.WorldUtil;
import togos.networkrts.experimental.game19.world.sim.Simulator;
import togos.networkrts.experimental.shape.TCircle;
import togos.networkrts.repo.BlobRepository;
import togos.networkrts.ui.ImageCanvas;
import togos.networkrts.util.Getter;
import togos.networkrts.util.ImageGetter;

public class ServerClientDemo
{
	static class Scene {
		public final Layer layer;
		public final double layerX, layerY, layerDistance;
		
		public Scene( Layer layer, double x, double y, double dist ) {
			this.layer = layer;
			this.layerX = x;
			this.layerY = y;
			this.layerDistance = dist;
		}
	}
	
	static class SceneCanvas extends ImageCanvas {
		private static final long serialVersionUID = 1L;

		BufferedImage sceneBuffer = new BufferedImage(512,384,BufferedImage.TYPE_INT_ARGB);
		
		protected final Renderer renderer;
		public SceneCanvas( Getter<BufferedImage> imageSource ) {
			renderer = new Renderer(imageSource);
		}
		
		Color sceneBackgroundColor = Color.BLUE;
		
		public void setScene( Scene s ) {
			synchronized( sceneBuffer ) {
				Graphics g = sceneBuffer.getGraphics();
				g.setColor( sceneBackgroundColor );
				g.fillRect( 0, 0, sceneBuffer.getWidth(), sceneBuffer.getHeight() );
				renderer.draw( s.layer, s.layerX, s.layerY, s.layerDistance, g, 32, sceneBuffer.getWidth()/2, sceneBuffer.getHeight()/2 );
			}
			setImage(sceneBuffer);
		}
	}
	
	static class Client {
		SceneCanvas sceneCanvas;
		
		public Client( Getter<BufferedImage> imageSource ) {
			sceneCanvas = new SceneCanvas(imageSource);
			sceneCanvas.setBackground(Color.BLACK);
		}
		
		public void setScene( Scene s ) {
			sceneCanvas.setScene(s);
		}
		
		public void startUi() {
			final Frame f = new Frame("Game19 Render Demo");
			f.add(sceneCanvas);
			f.addWindowListener(new WindowAdapter() {
				@Override public void windowClosing(WindowEvent evt) {
					f.dispose();
				}
			});
			f.pack();
			f.setVisible(true);
		}
	}
	
	public static void main( String[] args ) throws Exception {
		BlobRepository repo = new BlobRepository(new File(".ccouch"));
		ImageGetter imageGetter = new ImageGetter(repo.toBlobGetter());
		final Client c = new Client(imageGetter);
		c.startUi();
		
		Thread simulatorThread = new Thread("World updater") {
			@Override public void run() {
				try {
					_run();
				} catch( Exception e ) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			
			public void _run() throws Exception {
				ResourceContext rc = new ResourceContext(new File(".ccouch"));
				Block bricks = new Block(rc.storeImageHandle(new File("tile-images/dumbrick1.png")), Block.FLAG_SOLID, NoBehavior.instance);
				Block dude = new Block(rc.storeImageHandle(new File("tile-images/dude.png")), Block.FLAG_SOLID, new RandomWalkBehavior(0x0102, 1));
				
				int worldSizePower = 24;
				int worldDataOrigin = -(1<<(worldSizePower-1));
				
				WorldNode n = WorldUtil.createSolid(bricks.stack, worldSizePower);
				n = WorldUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( -2, -2, 4 ), new SolidNodeFiller( BlockStack.EMPTY ));
				n = WorldUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( +2, +2, 4 ), new SolidNodeFiller( BlockStack.EMPTY ));
				n = WorldUtil.updateBlockStackAt( n, worldDataOrigin, worldDataOrigin, worldSizePower, -2, -2, dude, null);
				n = WorldUtil.updateBlockStackAt( n, worldDataOrigin, worldDataOrigin, worldSizePower, -3, -2, dude, null);
				n = WorldUtil.updateBlockStackAt( n, worldDataOrigin, worldDataOrigin, worldSizePower, -4, -2, dude, null);
				final Simulator sim = new Simulator();
				sim.setRoot( n, worldDataOrigin, worldDataOrigin, worldSizePower );
				
				long simTime = 0;
				while(true) {
					System.err.println("Update to "+simTime+"...");
					sim.update(simTime, Message.EMPTY_LIST);
					n = sim.getRootNode();
					
					LayerData layerData = new LayerData( 16, 16, 1 );
					WorldConverter.nodeToLayerData( n, worldDataOrigin, worldDataOrigin, 0, 1<<worldSizePower, layerData, -8, -8, 16, 16 );
					Layer l = new Layer( layerData, -8, -8, null, 0, 0, 0 );
					Scene s = new Scene( l, 0, 0, 1 );
					c.setScene(s);
					
					Thread.sleep(100);
					simTime += 1;
				}
			}
		};
		simulatorThread.setDaemon(true);
		simulatorThread.start();
	}
}
