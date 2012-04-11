package togos.networkrts.simplesim;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import togos.service.InterruptableSingleThreadedService;
import togos.service.ServiceManager;

/**
 * Proof-of-concept simulator that simulates a simple tile-based world
 * represented by immutable objects with behavior. 
 */
public class SimpleSim
{
	static class Event {
		// These ones are not really events or commands, but markers
		public static final int NEW_ENTITY_MAP = -2;
		public static final int END_LIST = -1;
		
		public static final int TICK = 0;
		public static final int ATTEMPT_MOVE = 1;
		
		final String targetId;
		final int verb;
		final int arg;
		final Object payload;
		
		public Event( String targetId, int verb, int arg, Object payload ) {
			this.targetId = targetId;
			this.verb = verb;
			this.arg = arg;
			this.payload = payload;
		}
	}
	
	/**
	 * Returned by Behaviors in response to events
	 */
	static class CommandSet {
		public static final CommandSet EMPTY = new CommandSet( Collections.EMPTY_LIST, null );
		
		final List commands;
		/** May be null, in which case old behavior should remain. */
		final Behavior newBehavior;
		
		public CommandSet( List cmds, Behavior newb ) {
			this.commands = cmds;
			this.newBehavior = newb;
		}
	}
	
	static class ObjectCommandSet extends CommandSet {
		final String objectId;
		
		public ObjectCommandSet( String objId, List cmds, Behavior newBehavior ) {
			super( cmds, newBehavior );
			this.objectId = objId;
		}
	}
	
	interface Behavior {
		public static final Behavior NONE = new Behavior() {
			public CommandSet onEvent(Event e) {
				return CommandSet.EMPTY;
			}
		};
		
		CommandSet onEvent( Event e );
	}
	
	static class Entity {
		public final int x, y;
		public final Behavior behavior;
		public final Map attrs;
		
		public Entity( int x, int y, Behavior behavior, Map attrs ) {
			this.x = x;
			this.y = y;
			this.behavior = behavior;
			this.attrs = attrs;
		}
	}
	
	static class Block {
		public static final Block EMPTY = new Block( null, Color.BLACK );
		public static final Block STONE = new Block( null, Color.DARK_GRAY );
		
		public final String entityId;
		public final Color color;
		
		public Block( String entityId, Color c ) {
			this.entityId = entityId;
			this.color = c;
		}
	}
	
	static class TileGrid {
		public static final TileGrid EMPTY = new TileGrid(0, 0, new Block[]{ Block.EMPTY });
		
		public final int width, height; // Must be powers of 2!!!
		public final Block[] blocks;
		
		public Block get( int x, int y ) {
			return blocks[x+y*width];
		}
		public void put( int x, int y, Block b ) {
			blocks[x+y*width] = b;
		}
		
		public TileGrid( TileGrid old, Block[] blocks ) {
			this.width = old.width;
			this.height = old.height;
			this.blocks = blocks;
		}
		
		public TileGrid( int widthPower, int heightPower, Block[] blocks ) {
			if( widthPower < 0 || widthPower > 10 ) {
				throw new RuntimeException("Map#widthPower is out of range (0..10): "+widthPower);
			}
			if( heightPower < 0 || heightPower > 10 ) {
				throw new RuntimeException("Map#heightPower is out of range (0..10): "+heightPower);
			}
			this.width  = (1 <<  widthPower);
			this.height = (1 << heightPower);
			if( width * height != blocks.length ) {
				throw new RuntimeException("blocks array should have length = width ("+width+") * height ("+height+"), but is "+blocks.length);
			}
			this.blocks = blocks;
		}
	}
	
	static class WorldState {
		public static final WorldState EMPTY = new WorldState( TileGrid.EMPTY, Collections.EMPTY_MAP );
		
		public final TileGrid tileGrid;
		public final Map entities;
		
		public WorldState( TileGrid m, Map entities ) {
			this.tileGrid = m;
			this.entities = entities;
		}
	}
	
	static abstract class InterruptableLoopingService extends InterruptableSingleThreadedService {
		protected abstract void runOneIteration() throws InterruptedException;
		
		protected void _run() throws InterruptedException {
			while( !Thread.interrupted() ) {
				runOneIteration();
			}
		}
	}
	
	static class EventHandler extends InterruptableLoopingService {
		public BlockingQueue inputEventQueue;
		public BlockingQueue outputCommandQueue;
		
		protected void runOneIteration() throws InterruptedException {
			Event evt = (Event)inputEventQueue.take();
		}
	}
	
	static class PhysicsRunner extends InterruptableLoopingService {
		public BlockingQueue inputWorldStateQueue;
		public BlockingQueue eventQueue;
		public BlockingQueue outputWorldStateQueue;
		
		long lastRunTime = -1;
		long interval = 100;
		
		static final Event TICK = new Event( null, Event.TICK, 0, null );
		
		protected final void updateEntityPosition( Map entities, String id, int nx, int ny ) {
			Entity e = (Entity)entities.get(id);
			if( e == null ) return;
			
			e = new Entity( nx, ny, e.behavior, e.attrs );
			entities.put( id, e );
		}
		
		protected void runOneIteration() throws InterruptedException {
			long curTime = System.currentTimeMillis();
			
			if( lastRunTime != -1 && lastRunTime + interval > curTime ) {
				Thread.sleep( lastRunTime + interval - curTime );
			}
			
			lastRunTime = System.currentTimeMillis();
			
			WorldState s = (WorldState)inputWorldStateQueue.take();
			TileGrid grid = s.tileGrid;
			
			Block[] newBlocks = new Block[grid.width*grid.height];
			for( int y=grid.height-1; y>=0; --y ) {
				for( int x=grid.width-1; x>=0; --x ) {
					newBlocks[x+y*grid.width] = grid.blocks[x+y*grid.width]; 
				}
			}
			
			Map newEntities = new HashMap(s.entities);
			
			for( int y=grid.height-1; y>=0; --y ) {
				for( int x=grid.width-1; x>=0; --x ) {
					final Block b = grid.blocks[x+y*grid.width];
					CommandSet cs;
					Entity ent = (Entity)newEntities.get(b.entityId);
					if( ent != null && ent.behavior != Behavior.NONE ) {
						if( (cs = ent.behavior.onEvent(TICK)) != CommandSet.EMPTY ) {
							int movementDir = -1;
							for( Iterator ci=cs.commands.iterator(); ci.hasNext(); ) {
								Event e = (Event)ci.next();
								if( e.verb == Event.ATTEMPT_MOVE ) {
									movementDir = e.arg;
								}
							}
							
							Block newBlock = new Block( b.entityId, b.color );
							newBlocks[x+y*grid.width] = newBlock; // but see movement handling, below
							
							int dx, dy;
							switch( movementDir ) {
							case( 0 ): dx =  1; dy =  0; break;
							case( 2 ): dx =  0; dy =  1; break;
							case( 4 ): dx = -1; dy =  0; break;
							case( 6 ): dx =  0; dy = -1; break;
							default:   dx =  0; dy =  0; break;
							}
							
							if( dx != 0 || dy != 0 ) {
								int nx = (x+dx) & (grid.width-1);
								int ny = (y+dy) & (grid.height-1);
								if( newBlocks[nx+ny*grid.width] == Block.EMPTY ) {
									newBlocks[nx+ny*grid.width] = newBlock;
									newBlocks[x+y*grid.width] = Block.EMPTY;
									updateEntityPosition(newEntities, b.entityId, nx, ny);
								}
							}
						}
					}
				}
			}
			
			outputWorldStateQueue.put( new WorldState(new TileGrid(grid, newBlocks), newEntities) );
		}
	}
	
	interface WorldUpdatable {
		public void setWorldState( WorldState s );
	}
	
	static class MapCanvas extends Canvas implements WorldUpdatable {
		WorldState state = WorldState.EMPTY;
		
		public synchronized void setWorldState( WorldState s ) {
			if( s == state ) return;
			state = s;
			repaint();
		}
		
		public void paint( Graphics g ) {
			final TileGrid s = state.tileGrid;
			
			for( int y=s.height-1; y>=0; --y ) {
				for( int x=s.width-1; x>=0; --x ) {
					g.setColor( s.blocks[x+y*s.width].color );
					g.fillRect(x * 16, y * 16, 16, 16);
				}
			}
		}
		
		public void update( Graphics g ) {
			paint(g);
		}
	}
	
	static class MapUpdater extends InterruptableSingleThreadedService {
		public BlockingQueue inputWorldStateQueue;
		public BlockingQueue outputWorldStateQueue;
		public WorldUpdatable updateListener;
		
		public void _run() throws InterruptedException {
			while( !Thread.interrupted() ) {
				WorldState s = (WorldState)inputWorldStateQueue.take();
				outputWorldStateQueue.put(s);
				updateListener.setWorldState(s);
			}
		}
	}
	
	protected static void addEntity( WorldState s, String entityId, Entity e, Color c ) {
		s.tileGrid.put( e.x, e.y, new Block(entityId,c) );
		s.entities.put( entityId, e );
	}
	
	public static void main( String[] args ) throws InterruptedException {
		final ServiceManager sman = new ServiceManager();
		
		final Behavior wanderator = new Behavior() {
			public CommandSet onEvent(Event e) {
				List cmds = new ArrayList();
				cmds.add( new Event(e.targetId, Event.ATTEMPT_MOVE, new Random().nextInt(8), null ));
				return new CommandSet( cmds, this );
			}
		};
		
		byte[] mapmap = new byte[] {
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1,
			1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
			1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1,
			1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1,
			1, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1,
			1, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1,
			1, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1,
			1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1,
			1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 0, 0, 1,
			1, 0, 0, 1, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1,
			0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0,
			0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0,
			1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
			1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		};
		
		Block[] blockPal = new Block[] {
			Block.EMPTY,
			Block.STONE,
		};
		
		Block[] mapBlocks = new Block[256];
		TileGrid tileGrid = new TileGrid(4,4,mapBlocks);
		Map entities = new HashMap();
		WorldState ws = new WorldState( tileGrid, entities );
		for( int i=0; i<mapmap.length; ++i ) {
			mapBlocks[i] = blockPal[mapmap[i]];
		}
		
		Random r = new Random();
		for( int i=0; i<10; ++i ) {
			addEntity( ws, "wanderer"+i, new Entity(r.nextInt(16), r.nextInt(16), wanderator, Collections.EMPTY_MAP), new Color( 0xFF000000 | ((r.nextInt(0x80)+0x80) << 16) | ((r.nextInt(0x80)+0x80) << 8), true) );
		}
		
		PhysicsRunner pr = new PhysicsRunner();
		
		MapUpdater mu = new MapUpdater();
		mu.inputWorldStateQueue = pr.outputWorldStateQueue = new LinkedBlockingQueue();
		mu.outputWorldStateQueue = pr.inputWorldStateQueue = new LinkedBlockingQueue();
		pr.inputWorldStateQueue.put(ws);
		
		final Frame f = new Frame("SimpleSim");
		final MapCanvas c = new MapCanvas();
		mu.updateListener = c;
		c.setPreferredSize( new Dimension(512,384) );
		c.setWorldState( ws );
		f.add( c );
		f.pack();
		f.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				sman.halt();
				f.dispose();
			}
		});
		
		sman.add(pr);
		sman.add(mu);
		sman.start();
		
		f.setVisible( true );
	}
}
