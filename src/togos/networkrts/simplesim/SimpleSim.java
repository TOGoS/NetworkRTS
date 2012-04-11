package togos.networkrts.simplesim;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
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
	static class Entity {
		public final int x, y;
		public final Map attrs;
		
		public Entity( int x, int y, Map attrs ) {
			this.x = x;
			this.y = y;
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
		
		public TileGrid( TileGrid grid ) {
			this( grid, (Block[])Arrays.copyOf(grid.blocks, grid.blocks.length));
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
		public static final WorldState EMPTY = new WorldState( TileGrid.EMPTY, Collections.EMPTY_MAP, Collections.EMPTY_MAP, Collections.EMPTY_MAP );
		
		public final TileGrid tileGrid;
		public final Map entities;
		public final Map behaviors;
		public final Map commandLists;
		
		public WorldState( TileGrid m, Map entities, Map behaviors, Map commandLists ) {
			this.tileGrid = m;
			this.entities = entities;
			this.behaviors = behaviors;
			this.commandLists = commandLists;
		}
	}
	
	static class Event {
		public static final String TARGET_ALL = "(all)";
		
		// These ones are not really events or commands, but markers
		public static final int NEW_BEHAVIOR_MAP = -2;
		public static final int END_LIST = -1;
		
		public static final int TICK = 0;
		
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
	
	static class Command {
		public static final int ATTEMPT_MOVE = 1;
		
		final int verb;
		final int arg;
		final Object payload;
		
		public Command( int verb, int arg, Object payload ) {
			this.verb = verb;
			this.arg = arg;
			this.payload = payload;
		}
	}
	
	/**
	 * Returned by Behaviors in response to events
	 */
	static class BehaviorResult {
		public static final BehaviorResult EMPTY = new BehaviorResult( Collections.EMPTY_LIST, null );
		
		final List commands;
		/** May be null, in which case old behavior should remain. */
		final Behavior newBehavior;
		
		public BehaviorResult( List cmds, Behavior newb ) {
			this.commands = cmds;
			this.newBehavior = newb;
		}
	}
	
	interface Behavior {
		public static final Behavior NONE = new Behavior() {
			public BehaviorResult onEvent(Event e) {
				return BehaviorResult.EMPTY;
			}
		};
		
		BehaviorResult onEvent( Event e );
	}
	
	static class BehaviorResults {
		public final Map commandLists;
		public final Map newBehaviors;
		
		public BehaviorResults( Map cs, Map nb ) {
			this.commandLists = cs;
			this.newBehaviors = nb;
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
	
	static class BehaviorRunner extends InterruptableLoopingService {
		public BlockingQueue inputEventQueue;
		public BlockingQueue outputResultsQueue;
		
		protected Map commandLists;
		protected Map behaviors;
		
		protected void onCommandSet( String entityId, BehaviorResult s ) {
			if( s.commands.size() > 0 ) {
				List ocs = (List)commandLists.get( entityId );
				if( ocs == null ) {
					commandLists.put( entityId, new ArrayList(s.commands) );
				} else {
					ocs.addAll( s.commands );
				}
			}
			behaviors.put( entityId, s.newBehavior );
		}
		
		protected void runOneIteration() throws InterruptedException {
			Event evt = (Event)inputEventQueue.take();
			if( evt.targetId == null ) {
				switch( evt.verb ) {
				case( Event.NEW_BEHAVIOR_MAP ):
					commandLists = new HashMap();
					behaviors = new HashMap( (Map)evt.payload );
					break;
				case( Event.END_LIST ):
					outputResultsQueue.put( new BehaviorResults(commandLists, behaviors) );
					break;
				}
			} else if( Event.TARGET_ALL.equals(evt.targetId) ) {
				for( Iterator i=behaviors.entrySet().iterator(); i.hasNext(); ) {
					Map.Entry e = (Map.Entry)i.next();
					onCommandSet( (String)e.getKey(), ((Behavior)e.getValue()).onEvent(evt) );
				}
			} else {
				Behavior b;
				if( evt.targetId != null && (b = (Behavior)behaviors.get(evt.targetId)) != null ) {
					onCommandSet(evt.targetId, b.onEvent(evt));
				}
			}
		}
	}
	
	static class PhysicsRunner extends InterruptableLoopingService {
		public BlockingQueue inputWorldStateQueue;
		public BlockingQueue toBehaviorRunner;
		public BlockingQueue fromBehaviorRunner;
		public BlockingQueue outputWorldStateQueue;
		
		long lastRunTime = -1;
		long interval = 100;
		
		static final Event GLOBAL_TICK = new Event( Event.TARGET_ALL, Event.TICK, 0, null );
		
		protected final void updateEntityPosition( Map entities, String id, int nx, int ny ) {
			Entity e = (Entity)entities.get(id);
			if( e == null ) return;
			
			e = new Entity( nx, ny, e.attrs );
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
			
			toBehaviorRunner.put( new Event(null, Event.NEW_BEHAVIOR_MAP, 0, s.behaviors) );
			toBehaviorRunner.put( GLOBAL_TICK );

			// Create mutable copies of the old state's blocks and entities
			Map newEntities = new HashMap(s.entities);
			TileGrid newTileGrid = new TileGrid(grid);
			
			for( Iterator i=s.commandLists.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry e = (Map.Entry)i.next();
				String entityId = (String)e.getKey();
				List commands = (List)e.getValue();
				int movementDir = -1;
				for( Iterator j=commands.iterator(); j.hasNext(); ) {
					Command cmd = (Command)j.next();
					switch( cmd.verb ) {
					case( Command.ATTEMPT_MOVE ):
						movementDir = cmd.arg;
						break;
					}
				}
				
				Entity ent = (Entity)newEntities.get(entityId);
				Block entityBlock = newTileGrid.get(ent.x, ent.y);
				
				int dx, dy;
				switch( movementDir ) {
				case( 0 ): dx =  1; dy =  0; break;
				case( 2 ): dx =  0; dy =  1; break;
				case( 4 ): dx = -1; dy =  0; break;
				case( 6 ): dx =  0; dy = -1; break;
				default:   dx =  0; dy =  0; break;
				}
				
				if( dx != 0 || dy != 0 ) {
					int nx = (ent.x+dx) & (grid.width-1);
					int ny = (ent.y+dy) & (grid.height-1);
					if( newTileGrid.get(nx, ny) == Block.EMPTY ) {
						newTileGrid.put(nx, ny, entityBlock);
						newTileGrid.put(ent.x, ent.y, Block.EMPTY);
						updateEntityPosition(newEntities, entityBlock.entityId, nx, ny);
					}
				}
			}
			
			toBehaviorRunner.put( new Event(null, Event.END_LIST, 0, null) );
			BehaviorResults bRes = (BehaviorResults)fromBehaviorRunner.take();
			WorldState newWorldState = new WorldState(newTileGrid, newEntities, bRes.newBehaviors, bRes.commandLists);
			
			outputWorldStateQueue.put( newWorldState );
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
	
	protected static void addEntity( WorldState s, String entityId, Entity e, Behavior beh, Color c ) {
		s.tileGrid.put( e.x, e.y, new Block(entityId,c) );
		s.entities.put( entityId, e );
		s.behaviors.put( entityId, beh );
	}
	
	public static void main( String[] args ) throws InterruptedException {
		final ServiceManager sman = new ServiceManager();
		
		final Behavior wanderator = new Behavior() {
			public BehaviorResult onEvent(Event e) {
				List cmds = new ArrayList();
				cmds.add( new Command(Command.ATTEMPT_MOVE, new Random().nextInt(8), null ));
				return new BehaviorResult( cmds, this );
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
		WorldState ws = new WorldState( tileGrid, new HashMap(), new HashMap(), new HashMap() );
		for( int i=0; i<mapmap.length; ++i ) {
			mapBlocks[i] = blockPal[mapmap[i]];
		}
		
		Random r = new Random();
		for( int i=0; i<10; ++i ) {
			addEntity( ws, "wanderer"+i, new Entity(r.nextInt(16), r.nextInt(16), Collections.EMPTY_MAP), wanderator,
					new Color( 0xFF000000 | ((r.nextInt(0x80)+0x80) << 16) | ((r.nextInt(0x80)+0x80) << 8), true) );
		}
		
		PhysicsRunner pr = new PhysicsRunner();
		BehaviorRunner br = new BehaviorRunner();
		pr.toBehaviorRunner = br.inputEventQueue = new LinkedBlockingQueue();
		pr.fromBehaviorRunner = br.outputResultsQueue = new LinkedBlockingQueue();
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
		sman.add(br);
		sman.add(mu);
		sman.start();
		
		f.setVisible( true );
	}
}
