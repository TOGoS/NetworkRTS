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
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import togos.networkrts.experimental.game19.Renderer;
import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.scene.TileLayerData;
import togos.networkrts.experimental.game19.scene.VisibilityChecker;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStackNode;
import togos.networkrts.experimental.game19.world.IDGenerator;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.NodePosition;
import togos.networkrts.experimental.game19.world.QuadTreeNode;
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
import togos.networkrts.ui.ImageCanvas;

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

		BufferedImage sceneBuffer = new BufferedImage(512, 384, BufferedImage.TYPE_INT_RGB); // Much faster than ARGB!
		int cellScale = 24;
		
		protected final Renderer renderer;
		public SceneCanvas( ResourceContext resourceContext ) {
			renderer = new Renderer(resourceContext);
		}
		
		Color sceneBackgroundColor = new Color(0.2f, 0, 0);
		
		public void setScene( Scene s ) {
			synchronized( sceneBuffer ) {
				Graphics g = sceneBuffer.getGraphics();
				g.setClip(0, 0, sceneBuffer.getWidth(), sceneBuffer.getHeight());
				g.setColor( sceneBackgroundColor );
				g.fillRect( 0, 0, sceneBuffer.getWidth(), sceneBuffer.getHeight() );
				renderer.draw( s.layer, s.layerX, s.layerY, s.layerDistance, g, cellScale, sceneBuffer.getWidth()/2, sceneBuffer.getHeight()/2 );
			}
			setImage(sceneBuffer);
		}
	}
	
	static class Client {
		SceneCanvas sceneCanvas;
		public Queue<Message> messageQueue;
		
		public Client( ResourceContext resourceContext ) {
			sceneCanvas = new SceneCanvas(resourceContext);
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
		
		final ResourceContext resourceContext = new ResourceContext(new File(".ccouch"));
		final Client c = new Client(resourceContext);
		final int playerBlockId = idGenerator.newId();
		final int ballBlockId = idGenerator.newId();
		final int dudeBlockId = idGenerator.newId();
		c.startUi();
		c.sceneCanvas.addKeyListener(new KeyListener() {
			boolean[] keysDown = new boolean[8];
			int oldDir = -2;
			
			protected boolean dkd( int dir ) {
				return keysDown[dir] || keysDown[dir+4];
			}
			
			final int[] dirs = new int[] {
				5,  6, 7,
				4, -1, 0,
				3,  2, 1
			};
			
			protected int dir( int dirX, int dirY ) {
				return dirs[(dirY+1)*3 + dirX+1];
			}
			
			protected void keySomething( int keyCode, boolean state ) {
				int dkCode;
				switch( keyCode ) {
				case KeyEvent.VK_W: dkCode = 3; break;
				case KeyEvent.VK_A: dkCode = 2; break;
				case KeyEvent.VK_S: dkCode = 1; break;
				case KeyEvent.VK_D: dkCode = 0; break;
				case KeyEvent.VK_UP: dkCode = 7; break;
				case KeyEvent.VK_LEFT: dkCode = 6; break;
				case KeyEvent.VK_DOWN: dkCode = 5; break;
				case KeyEvent.VK_RIGHT: dkCode = 4; break;
				default: return; // Not a key we care about
				}
				
				keysDown[dkCode] = state;
				int dirX, dirY;
				if( dkd(0) && !dkd(2) ) {
					dirX = 1;
				} else if( dkd(2) && !dkd(0) ) {
					dirX = -1;
				} else {
					dirX = 0;
				}
				if( dkd(1) && !dkd(3) ) {
					dirY = 1;
				} else if( dkd(3) && !dkd(1) ) {
					dirY = -1;
				} else {
					dirY = 0;
				}
				
				int dir = dir(dirX, dirY);
				if( dir != oldDir ) {
					messageQueue.add(new Message(playerBlockId, TBoundless.INSTANCE, MessageType.INCOMING_PACKET, Integer.valueOf(dir) ));
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
				ImageHandle brickImage = resourceContext.storeImageHandle(new File("tile-images/dumbrick1.png"));
				ImageHandle dudeImage = resourceContext.storeImageHandle(new File("tile-images/dude.png"));
				ImageHandle ballImage = resourceContext.storeImageHandle(new File("tile-images/stupid-ball.png"));
				
				Block bricks = new Block(BitAddresses.BLOCK_SOLID|BitAddresses.BLOCK_OPAQUE, brickImage, NoBehavior.instance);
				Block dude = new Block(dudeBlockId|BitAddresses.BLOCK_SOLID, dudeImage, new RandomWalkBehavior(3, 1));
				Block player = new Block(playerBlockId|BitAddresses.BLOCK_SOLID, dudeImage, new WalkingBehavior(2, 0, -1));
				Block stupidBall = new Block(ballBlockId|BitAddresses.BLOCK_PHYS|BitAddresses.BLOCK_SOLID, ballImage, NoBehavior.instance);
				
				int worldSizePower = 24;
				int worldDataOrigin = -(1<<(worldSizePower-1));
				
				WorldNode n = QuadTreeNode.createHomogeneous(bricks.stack, worldSizePower);
				n = WorldUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( -2, -2, 4 ), new SolidNodeFiller( BlockStackNode.EMPTY ));
				n = WorldUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( +2, +2, 4 ), new SolidNodeFiller( BlockStackNode.EMPTY ));
				
				Random r = new Random();
				for( int i=0; i<50; ++i ) {
					n = WorldUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( r.nextGaussian()*20, r.nextGaussian()*20, r.nextDouble()*8 ), new SolidNodeFiller( BlockStackNode.EMPTY ));
				}
				
				n = WorldUtil.updateBlockStackAt( n, worldDataOrigin, worldDataOrigin, worldSizePower, -2, -2, dude, null);
				n = WorldUtil.updateBlockStackAt( n, worldDataOrigin, worldDataOrigin, worldSizePower, -3, -2, dude, null);
				n = WorldUtil.updateBlockStackAt( n, worldDataOrigin, worldDataOrigin, worldSizePower, -4, -2, dude, null);
				n = WorldUtil.updateBlockStackAt( n, worldDataOrigin, worldDataOrigin, worldSizePower, -4, -0, player, null);
				n = WorldUtil.updateBlockStackAt( n, worldDataOrigin, worldDataOrigin, worldSizePower, -5, -0, stupidBall, null);
				final Simulator sim = new Simulator();
				sim.setRoot( n, worldDataOrigin, worldDataOrigin, worldSizePower );
				
				long simTime = 0;
				while(true) {
					Message m;
					while( (m = messageQueue.poll()) != null ) {
						sim.enqueueMessage(m);
					}
					
					sim.update(simTime);
					n = sim.getNode();
					
					NodePosition playerPosition = WorldUtil.findBlock(n, sim.getNodeX(), sim.getNodeY(), sim.getNodeSizePower(), playerBlockId);
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
					
					int ldWidth = 21;
					int ldHeight = 15;
					int ldCenterX = ldWidth/2;
					int ldCenterY = ldHeight/2;
					
					TileLayerData layerData = new TileLayerData( ldWidth, ldHeight, 1 );
					WorldConverter.nodeToLayerData( n, worldDataOrigin, worldDataOrigin, 0, 1<<worldSizePower, layerData, intCenterX-ldCenterX, intCenterY-ldCenterY, ldWidth, ldHeight );
					VisibilityChecker.calculateAndApplyVisibility(layerData, ldCenterX, ldCenterY, 0);
					Layer l = new Layer( layerData, -ldWidth/2.0, -ldHeight/2.0, new Layer.VisibilityClip(-ldWidth/2.0, -ldHeight/2.0, ldWidth/2.0, ldHeight/2.0), false, null, 0, 0, 0 );
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
