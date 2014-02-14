package togos.networkrts.experimental.game19.demo;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import togos.networkrts.experimental.game19.Renderer;
import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.scene.LayerData;
import togos.networkrts.experimental.game19.scene.VisibilityChecker;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStack;
import togos.networkrts.experimental.game19.world.IDGenerator;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.NodePosition;
import togos.networkrts.experimental.game19.world.WorldNode;
import togos.networkrts.experimental.game19.world.WorldUtil;
import togos.networkrts.experimental.game19.world.beh.NoBehavior;
import togos.networkrts.experimental.game19.world.beh.RandomWalkBehavior;
import togos.networkrts.experimental.game19.world.beh.WalkingBehavior;
import togos.networkrts.experimental.game19.world.encoding.WorldConverter;
import togos.networkrts.experimental.game19.world.gen.SolidNodeFiller;
import togos.networkrts.experimental.game19.world.sim.Simulator;
import togos.networkrts.experimental.shape.TBoundless;
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
		public Queue<Message> messageQueue;
		
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
		final ConcurrentLinkedQueue<Message> messageQueue = new ConcurrentLinkedQueue<Message>(); 
		
		IDGenerator idGenerator = new IDGenerator();
		
		BlobRepository repo = new BlobRepository(new File(".ccouch"));
		ImageGetter imageGetter = new ImageGetter(repo.toBlobGetter());
		final Client c = new Client(imageGetter);
		final long playerBlockId = idGenerator.newBlockId();
		final long dudeBlockId = idGenerator.newBlockId();
		c.startUi();
		c.sceneCanvas.addKeyListener(new KeyListener() {
			boolean[] keysDown = new boolean[4];
			int oldDir = -2;
			
			protected void keySomething( int keyCode, boolean state ) {
				int dkCode;
				switch( keyCode ) {
				case KeyEvent.VK_W: dkCode = 3; break;
				case KeyEvent.VK_A: dkCode = 2; break;
				case KeyEvent.VK_S: dkCode = 1; break;
				case KeyEvent.VK_D: dkCode = 0; break;
				case KeyEvent.VK_V:
					messageQueue.add(new Message(playerBlockId, playerBlockId, TBoundless.INSTANCE, MessageType.INCOMING_PACKET, Integer.valueOf(129) ));
					return;
				case KeyEvent.VK_I:
					messageQueue.add(new Message(playerBlockId, playerBlockId, TBoundless.INSTANCE, MessageType.INCOMING_PACKET, Integer.valueOf(130) ));
					return;
				default: return; // Not a key we care about
				}
				
				keysDown[dkCode] = state;
				int dir;
				if( keysDown[0] && !keysDown[2] ) {
					dir = 0;
				} else if( keysDown[2] && !keysDown[0] ) {
					dir = 4;
				} else if( keysDown[1] && !keysDown[3] ) {
					dir = 2;
				} else if( keysDown[3] && !keysDown[1] ) {
					dir = 6;
				} else {
					dir = -1;
				}
				
				if( dir != oldDir ) {
					messageQueue.add(new Message(playerBlockId, playerBlockId, TBoundless.INSTANCE, MessageType.INCOMING_PACKET, Integer.valueOf(dir) ));
					oldDir = dir;
				}
			}
			
			@Override public void keyPressed(KeyEvent kevt) {
				keySomething(kevt.getKeyCode(), true);
			}

			@Override public void keyReleased(KeyEvent kevt) {
				keySomething(kevt.getKeyCode(), false);
			}

			@Override public void keyTyped(KeyEvent arg0) {}
		});
		c.messageQueue = messageQueue;
		
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
				
				ImageHandle brickImage = rc.storeImageHandle(new File("tile-images/dumbrick1.png"));
				ImageHandle dudeImage = rc.storeImageHandle(new File("tile-images/dude.png"));
				
				Block bricks = new Block(brickImage, Block.FLAG_SOLID|Block.FLAG_OPAQUE, NoBehavior.instance);
				Block dude = new Block(dudeImage, Block.FLAG_SOLID, new RandomWalkBehavior(dudeBlockId, 1));
				Block player = new Block(dudeImage, Block.FLAG_SOLID, new WalkingBehavior(playerBlockId, 0, 10, -1, dudeImage, brickImage));
				
				int worldSizePower = 24;
				int worldDataOrigin = -(1<<(worldSizePower-1));
				
				WorldNode n = WorldUtil.createSolid(bricks.stack, worldSizePower);
				n = WorldUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( -2, -2, 4 ), new SolidNodeFiller( BlockStack.EMPTY ));
				n = WorldUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( +2, +2, 4 ), new SolidNodeFiller( BlockStack.EMPTY ));
				n = WorldUtil.updateBlockStackAt( n, worldDataOrigin, worldDataOrigin, worldSizePower, -2, -2, dude, null);
				n = WorldUtil.updateBlockStackAt( n, worldDataOrigin, worldDataOrigin, worldSizePower, -3, -2, dude, null);
				n = WorldUtil.updateBlockStackAt( n, worldDataOrigin, worldDataOrigin, worldSizePower, -4, -2, dude, null);
				n = WorldUtil.updateBlockStackAt( n, worldDataOrigin, worldDataOrigin, worldSizePower, -4, -0, player, null);
				final Simulator sim = new Simulator();
				sim.setRoot( n, worldDataOrigin, worldDataOrigin, worldSizePower );
				
				long simTime = 0;
				while(true) {
					Message m;
					while( (m = messageQueue.poll()) != null ) {
						sim.enqueueMessage(m);
					}
					
					sim.update(simTime);
					n = sim.getRootNode();
					
					NodePosition playerPosition = WorldUtil.findBlock(n, sim.getRootX(), sim.getRootY(), sim.getRootSizePower(), playerBlockId, playerBlockId);
					double centerX, centerY;
					if( playerPosition != null ) {
						centerX = playerPosition.getCenterX();
						centerY = playerPosition.getCenterY();
					} else {
						centerX = 0;
						centerY = 0;
					}
					
					int intCenterX = (int)Math.floor(centerX);
					int intCenterY = (int)Math.floor(centerY);
					
					LayerData layerData = new LayerData( 17, 17, 1 );
					WorldConverter.nodeToLayerData( n, worldDataOrigin, worldDataOrigin, 0, 1<<worldSizePower, layerData, intCenterX-8, intCenterY-8, 17, 17 );
					VisibilityChecker.calculateAndApplyVisibility(layerData, 8, 8, 0);
					Layer l = new Layer( layerData, -8, -8, null, 0, 0, 0 );
					Scene s = new Scene( l, 0, 0, 1 );
					c.setScene(s);
					
					Thread.sleep(40);
					simTime += 1;
				}
			}
		};
		simulatorThread.setDaemon(true);
		simulatorThread.start();
	}
}
