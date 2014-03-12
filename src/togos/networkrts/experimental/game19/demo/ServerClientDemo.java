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
import java.util.concurrent.LinkedBlockingQueue;

import togos.networkrts.experimental.game19.Renderer;
import togos.networkrts.experimental.game19.ResourceContext;
import togos.networkrts.experimental.game19.scene.ImageHandle;
import togos.networkrts.experimental.game19.scene.Layer;
import togos.networkrts.experimental.game19.sim.AsyncTask;
import togos.networkrts.experimental.game19.sim.NonTileUpdateContext;
import togos.networkrts.experimental.game19.sim.Simulator;
import togos.networkrts.experimental.game19.sim.UpdateContext;
import togos.networkrts.experimental.game19.world.BitAddresses;
import togos.networkrts.experimental.game19.world.Block;
import togos.networkrts.experimental.game19.world.BlockStackRSTNode;
import togos.networkrts.experimental.game19.world.IDGenerator;
import togos.networkrts.experimental.game19.world.Message;
import togos.networkrts.experimental.game19.world.Message.MessageType;
import togos.networkrts.experimental.game19.world.MessageSet;
import togos.networkrts.experimental.game19.world.NonTile;
import togos.networkrts.experimental.game19.world.NonTileBehavior;
import togos.networkrts.experimental.game19.world.QuadRSTNode;
import togos.networkrts.experimental.game19.world.RSTNode;
import togos.networkrts.experimental.game19.world.RSTUtil;
import togos.networkrts.experimental.game19.world.World;
import togos.networkrts.experimental.game19.world.beh.NoBehavior;
import togos.networkrts.experimental.game19.world.gen.SolidNodeFiller;
import togos.networkrts.experimental.gameengine1.index.EntitySpatialTreeIndex;
import togos.networkrts.experimental.shape.TBoundless;
import togos.networkrts.experimental.shape.TCircle;
import togos.networkrts.ui.ImageCanvas;
import togos.networkrts.util.BitAddressUtil;

public class ServerClientDemo
{
	// TODO: Move VisibilityClip from Layer into Scene.
	// TODO: Move cell visibility into Scene
	//   (so it can be drawn over everything, including sprites)
	
	/**
	 * A scene represents a portion of the world.
	 * The primary use for a scene is to send the part of the world
	 * that a character can see to a client to display.
	 */
	public static class Scene {
		public final Layer layer;
		public final Iterable<NonTile> nonTiles;
		// Point within the scene that should be centered on (usually the player)
		public final double poiX, poiY; 
		
		public Scene( Layer layer,  Iterable<NonTile> nonTiles, double poiX, double poiY ) {
			this.layer = layer;
			this.nonTiles = nonTiles;
			this.poiX = poiX;
			this.poiY = poiY;
		}
	}
	
	static class SceneCanvas extends ImageCanvas {
		private static final long serialVersionUID = 1L;

		protected BufferedImage sceneBuffer; // = new BufferedImage(512, 384, BufferedImage.TYPE_INT_RGB); // Much faster than ARGB!
		protected synchronized BufferedImage getSceneBuffer( int width, int height ) {
			if( sceneBuffer == null || sceneBuffer.getWidth() != width || sceneBuffer.getHeight() != height ) {
				return sceneBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			} else {
				return sceneBuffer;
			}
		}
		
		int cellScale = 24;
		
		protected final Renderer renderer;
		public SceneCanvas( ResourceContext resourceContext ) {
			renderer = new Renderer(resourceContext);
		}
		
		Color sceneBackgroundColor = new Color(0.2f, 0, 0);
		Scene scene;
		
		public synchronized void setScene( Scene s ) {
			this.scene = s;
			notifyAll();
		}
		
		public void redrawLoop() throws InterruptedException {
			Scene s = scene;
			while( true ) {
				synchronized(this) {
					while( scene == s || scene == null ) wait();
					s = scene;
				}
				int wid = getWidth(), hei = getHeight();
				//while( wid > 768 || hei > 512 ) {
				//	wid >>= 1; hei >>= 1;
				//}
				BufferedImage sb = getSceneBuffer(wid, hei);
				synchronized( sb ) {
					Graphics g = sb.getGraphics();
					g.setClip(0, 0, sb.getWidth(), sceneBuffer.getHeight());
					g.setColor( sceneBackgroundColor );
					g.fillRect( 0, 0, sb.getWidth(), sb.getHeight() );
					renderer.draw( s, -s.poiX, -s.poiY, 2, g, 32, sb.getWidth()/2, sb.getHeight()/2 );
					//renderer.draw( s.layer, s.layerX, s.layerY, s.layerDistance, g, cellScale, sceneBuffer.getWidth()/2, sceneBuffer.getHeight()/2 );
				}
				setImage(sb);
			}
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
			final Thread redrawThread = new Thread() {
				@Override public void run() {
					try {
						sceneCanvas.redrawLoop();
					} catch( InterruptedException e ) {
					}
				}
			};
			f.addWindowListener(new WindowAdapter() {
				@Override public void windowClosing(WindowEvent evt) {
					f.dispose();
					redrawThread.interrupt();
				}
			});
			f.pack();
			f.setVisible(true);
			redrawThread.start();
		}
	}
	
	public static void main( String[] args ) throws Exception {
		final LinkedBlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>(); 
		
		IDGenerator idGenerator = new IDGenerator();
		
		final ResourceContext resourceContext = new ResourceContext(new File(".ccouch"));
		final Client c = new Client(resourceContext);
		final int playerId = idGenerator.newId();
		final long playerNonTileBa = BitAddresses.forceType(BitAddresses.TYPE_NONTILE, playerId);
		final int clientId = idGenerator.newId();
		final long clientBa = BitAddresses.forceType(BitAddresses.TYPE_EXTERNAL, clientId);
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
					messageQueue.add(new Message(playerId, TBoundless.INSTANCE, MessageType.INCOMING_PACKET, Integer.valueOf(dir)));
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
				
				final Simulator sim;
				{
					World world;
					int worldSizePower = 24;
					int worldDataOrigin = -(1<<(worldSizePower-1));
					
					RSTNode n = QuadRSTNode.createHomogeneous(bricks.stack, worldSizePower);
					n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( -2, -2, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
					n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( +2, +2, 4 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
					
					Random r = new Random();
					for( int i=0; i<100; ++i ) {
						n = RSTUtil.fillShape( n, worldDataOrigin, worldDataOrigin, worldSizePower, new TCircle( r.nextGaussian()*20, r.nextGaussian()*20, r.nextDouble()*8 ), new SolidNodeFiller( BlockStackRSTNode.EMPTY ));
					}
					
					NonTileBehavior ballerBehavior = new NonTileBehavior() {
						Random r = new Random();
						@Override public NonTile update(NonTile nt, long time, World w, MessageSet messages, NonTileUpdateContext updateContext) {
							return nt.withPosition(time, nt.x+r.nextGaussian(), nt.y+r.nextGaussian());
						}
					};
					
					EntitySpatialTreeIndex<NonTile> nonTiles = new EntitySpatialTreeIndex<NonTile>();
					for( int i=0; i<10; ++i ) {
						NonTile baller = NonTile.create(0, r.nextGaussian()*10, r.nextGaussian()*10, ballImage, 2f, ballerBehavior);
						//nonTiles = nonTiles.with(baller);
					}
					
					class PlayerNTBehavior implements NonTileBehavior {
						final long messageBitAddress;
						final long clientBitAddress;
						
						public PlayerNTBehavior(long id, long clientId) {
							this.messageBitAddress = id;
							this.clientBitAddress = clientId;
						}
						
						@Override
						public NonTile update(NonTile nt, long time, World w,
							MessageSet messages, NonTileUpdateContext updateContext
						) {
							double newVx = nt.vx, newVy = nt.vy;
							for( Message m : messages ) {
								if( m.isApplicableTo(nt) && BitAddressUtil.rangeContains(m, messageBitAddress)) {
									Object p = m.payload;
									if( p instanceof Number ) {
										// Change our direction!
										switch( ((Number) p).intValue() ) {
										case -1: newVx =  0; newVy =  0; break;
										case  0: newVx = +1; newVy =  0; break;
										case  1: newVx = +1; newVy = +1; break;
										case  2: newVx =  0; newVy = +1; break;
										case  3: newVx = -1; newVy = +1; break;
										case  4: newVx = -1; newVy =  0; break;
										case  5: newVx = -1; newVy = -1; break;
										case  6: newVx =  0; newVy = -1; break;
										case  7: newVx = +1; newVy = -1; break;
										}
									}
								}
							}
							updateContext.startAsyncTask( new AsyncTask() {
								@Override
								public void run( UpdateContext ctx ) {
									// TODO Auto-generated method stub
								}
							});
							return nt.withVelocity(time-1, newVx, newVy);
						}
					}
					
					NonTile playerNonTile = NonTile.create(0, 0, 0, dudeImage, 3f, new PlayerNTBehavior(playerNonTileBa, clientBa)).withId(playerId);
					nonTiles = nonTiles.with(playerNonTile);
					
					world = new World(n, worldSizePower, nonTiles);
					sim = new Simulator( world );
				}
				
				sim.start();
				
				while( true ) {
					sim.enqueueMessage(messageQueue.take());
				}
				
				/*
				long simTime = 0;
				while(true) {
					Message m;
					while( (m = messageQueue.poll()) != null ) {
						sim.enqueueMessage(m);
					}
					
					sim.update(simTime);
					World world = sim.getWorld();
					int worldRadius = 1<<(world.rstSizePower-1);
					
					/*
					RSTNodePosition playerPosition = RSTUtil.findBlock(world.rst, -worldRadius, -worldRadius, world.rstSizePower);
					double centerX, centerY;
					if( playerPosition != null ) {
						centerX = playerPosition.getCenterX();
						centerY = playerPosition.getCenterY();
					} else {
						centerX = 0;
						centerY = 0;
					}
					* /
					double centerX = 0, centerY = 0;
					
					int intCenterX = (int)Math.floor(centerX);
					int intCenterY = (int)Math.floor(centerY);
					
					int ldWidth = 41;
					int ldHeight = 31;
					// center of layer data
					int ldCenterX = ldWidth/2;
					int ldCenterY = ldHeight/2;
					
					// TODO: Only collect the ones actually visible
					final List<NonTile> visibleNonTiles = new ArrayList<NonTile>();
					
					world.nonTiles.forEachEntity( EntityRanges.BOUNDLESS, new Visitor<NonTile>() {
						@Override public void visit( NonTile v ) {
							visibleNonTiles.add(v);
						}
					});
					
					// There are various ways to go about this:
					// - do visibility checks, send only visible area
					// - send nearby quadtree nodes
					// - send entire world
					
					boolean sendTiles = true;
					Layer l;
					VisibilityClip visibilityClip = new Layer.VisibilityClip(intCenterX-ldCenterX, intCenterY-ldCenterY, intCenterX-ldCenterX+ldWidth, intCenterY-ldCenterY+ldHeight);
					if( sendTiles ) {
						TileLayerData layerData = new TileLayerData( ldWidth, ldHeight, 1 );
						WorldConverter.nodeToLayerData( world.rst, -worldRadius, -worldRadius, 0, 1<<world.rstSizePower, layerData, intCenterX-ldCenterX, intCenterY-ldCenterY, ldWidth, ldHeight );
						VisibilityChecker.calculateAndApplyVisibility(layerData, ldCenterX, ldCenterY, 0, 16);
						l = new Layer( layerData, intCenterX-ldCenterX, intCenterY-ldCenterY, visibilityClip, false, null, 0, 0, 0 );
					} else {
						int size = 1<<world.rstSizePower;
						l = new Layer( new QuadTreeLayerData(world.rst, size), -size/2.0, -size/2.0, null, false, null, 0, 0, 0 );
					}
					c.setScene(new Scene( l, visibleNonTiles, centerX, centerY ));
					
					Thread.sleep(40);
					++simTime;
				}
				*/
			}
		};
		simulatorThread.setDaemon(true);
		simulatorThread.start();
	}
}
