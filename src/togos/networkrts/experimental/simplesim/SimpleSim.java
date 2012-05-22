package togos.networkrts.experimental.simplesim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
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

import togos.networkrts.awt.Apallit;
import togos.networkrts.awt.DoubleBufferedCanvas;
import togos.service.InterruptableSingleThreadedService;

/**
 * Proof-of-concept simulator that simulates a simple tile-based world
 * represented by immutable objects with behavior. 
 */
public class SimpleSim
{
	static class Entity {
		public final int x, y;
		public final Map<String,?> attrs;
		
		public Entity( int x, int y, Map<String,?> attrs ) {
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
			this( grid, Arrays.copyOf(grid.blocks, grid.blocks.length));
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
		public static final WorldState EMPTY = new WorldState( TileGrid.EMPTY,
			Collections.<String,Entity> emptyMap(),
			Collections.<String,Behavior> emptyMap(),
			Collections.<String,List<Command>> emptyMap() );
		
		public final TileGrid tileGrid;
		public final Map<String,Entity> entities;
		public final Map<String,Behavior> behaviors;
		public final Map<String,List<Command>> commandLists;
		
		public WorldState( TileGrid m, Map<String,Entity> entities,
				Map<String,Behavior> behaviors, Map<String,List<Command>> commandLists ) {
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
		public static final BehaviorResult EMPTY = new BehaviorResult( Collections.<Command>emptyList(), null );
		
		final List<Command> commands;
		/**
		 * May be null, in which case old behavior should remain.
		 * (this way we can re-use BehaviorResult.EMTPY for a lot
		 * of things).
		 * */
		final Behavior newBehavior;
		
		public BehaviorResult( List<Command> cmds, Behavior newb ) {
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
		public final Map<String,List<Command>> commandLists;
		public final Map<String,Behavior> newBehaviors;
		
		public BehaviorResults( Map<String,List<Command>> cs, Map<String, Behavior> nb ) {
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
		public BlockingQueue<Event> inputEventQueue;
		public BlockingQueue<BehaviorResults> outputResultsQueue;
		
		protected Map<String,List<Command>> commandLists;
		protected Map<String,Behavior> behaviors;
		
		protected void onCommandSet( String entityId, BehaviorResult s ) {
			if( s.commands.size() > 0 ) {
				List<Command> ocs = commandLists.get( entityId );
				if( ocs == null ) {
					commandLists.put( entityId, new ArrayList<Command>(s.commands) );
				} else {
					ocs.addAll( s.commands );
				}
			}
			if( s.newBehavior != null ) {
				behaviors.put( entityId, s.newBehavior );
			}
		}
		
		@SuppressWarnings("unchecked")
		protected void runOneIteration() throws InterruptedException {
			Event evt = inputEventQueue.take();
			if( evt.targetId == null ) {
				switch( evt.verb ) {
				case( Event.NEW_BEHAVIOR_MAP ):
					commandLists = new HashMap<String,List<Command>>();
					behaviors = new HashMap<String,Behavior>( (Map<String,Behavior>)evt.payload );
					break;
				case( Event.END_LIST ):
					outputResultsQueue.put( new BehaviorResults(commandLists, behaviors) );
					break;
				}
			} else if( Event.TARGET_ALL.equals(evt.targetId) ) {
				for( Iterator<Map.Entry<String,Behavior>> i=behaviors.entrySet().iterator(); i.hasNext(); ) {
					Map.Entry<String,Behavior> e = i.next();
					onCommandSet( e.getKey(), e.getValue().onEvent(evt) );
				}
			} else {
				Behavior b;
				if( evt.targetId != null && (b = behaviors.get(evt.targetId)) != null ) {
					onCommandSet(evt.targetId, b.onEvent(evt));
				}
			}
		}
	}
	
	static class PhysicsRunner extends InterruptableLoopingService {
		public BlockingQueue<WorldState> inputWorldStateQueue;
		public BlockingQueue<Event> toBehaviorRunner;
		public BlockingQueue<BehaviorResults> fromBehaviorRunner;
		public BlockingQueue<WorldState> outputWorldStateQueue;
		
		long lastRunTime = -1;
		long interval = 100;
		
		static final Event GLOBAL_TICK = new Event( Event.TARGET_ALL, Event.TICK, 0, null );
		
		protected final void updateEntityPosition( Map<String, Entity> entities, String id, int nx, int ny ) {
			Entity e = entities.get(id);
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
			
			WorldState s = inputWorldStateQueue.take();
			TileGrid grid = s.tileGrid;
			
			toBehaviorRunner.put( new Event(null, Event.NEW_BEHAVIOR_MAP, 0, s.behaviors) );
			toBehaviorRunner.put( GLOBAL_TICK );

			// Create mutable copies of the old state's blocks and entities
			Map<String, Entity> newEntities = new HashMap<String, Entity>(s.entities);
			TileGrid newTileGrid = new TileGrid(grid);
			
			for( Iterator<Map.Entry<String,List<Command>>> i=s.commandLists.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry<String,List<Command>> e = i.next();
				String entityId = e.getKey();
				List<Command> commands = e.getValue();
				int movementDir = -1;
				for( Iterator<Command> j=commands.iterator(); j.hasNext(); ) {
					Command cmd = j.next();
					switch( cmd.verb ) {
					case( Command.ATTEMPT_MOVE ):
						movementDir = cmd.arg;
						break;
					}
				}
				
				Entity ent = newEntities.get(entityId);
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
			BehaviorResults bRes = fromBehaviorRunner.take();
			WorldState newWorldState = new WorldState(newTileGrid, newEntities, bRes.newBehaviors, bRes.commandLists);
			
			outputWorldStateQueue.put( newWorldState );
		}
	}
	
	interface WorldUpdatable {
		public void setWorldState( WorldState s );
	}
	
	static class MapCanvas extends DoubleBufferedCanvas implements WorldUpdatable {
		private static final long serialVersionUID = 1L;
		
		WorldState state = WorldState.EMPTY;
		
		int tileWidth = 16, tileHeight = 16;
		
		public synchronized void setWorldState( WorldState s ) {
			if( s == state ) return;
			state = s;
			setPreferredSize( new Dimension(s.tileGrid.width * tileWidth, s.tileGrid.height * tileHeight) );
			repaint();
		}
		
		public void _paint( Graphics g ) {
			paintBackground( g );
			
			final TileGrid s = state.tileGrid;
			
			for( int y=s.height-1; y>=0; --y ) {
				for( int x=s.width-1; x>=0; --x ) {
					g.setColor( s.blocks[x+y*s.width].color );
					g.fillRect(x * tileWidth, y * tileHeight, tileWidth, tileHeight);
				}
			}
		}
	}
	
	static class MapUpdater extends InterruptableSingleThreadedService {
		public BlockingQueue<WorldState> inputWorldStateQueue;
		public BlockingQueue<WorldState> outputWorldStateQueue;
		public WorldUpdatable updateListener;
		
		public void _run() throws InterruptedException {
			while( !Thread.interrupted() ) {
				WorldState s = inputWorldStateQueue.take();
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
		final Behavior wanderator = new Behavior() {
			public BehaviorResult onEvent(Event e) {
				switch( e.verb ) {
				case( Event.TICK ):
					List<Command> cmds = new ArrayList<Command>();
					cmds.add( new Command(Command.ATTEMPT_MOVE, new Random().nextInt(8), null ));
					return new BehaviorResult( cmds, this );
				default:
					return BehaviorResult.EMPTY;
				}
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
		WorldState ws = new WorldState( tileGrid, new HashMap<String,Entity>(), new HashMap<String,Behavior>(), new HashMap<String,List<Command>>() );
		for( int i=0; i<mapmap.length; ++i ) {
			mapBlocks[i] = blockPal[mapmap[i]];
		}
		
		Random r = new Random();
		for( int i=0; i<10; ++i ) {
			addEntity( ws, "wanderer"+i, new Entity(r.nextInt(16), r.nextInt(16), Collections.<String,Object>emptyMap()), wanderator,
					new Color( 0xFF000000 | ((r.nextInt(0x80)+0x80) << 16) | ((r.nextInt(0x80)+0x80) << 8), true) );
		}
		
		PhysicsRunner pr = new PhysicsRunner();
		BehaviorRunner br = new BehaviorRunner();
		pr.toBehaviorRunner = br.inputEventQueue = new LinkedBlockingQueue<Event>();
		pr.fromBehaviorRunner = br.outputResultsQueue = new LinkedBlockingQueue<BehaviorResults>();
		MapUpdater mu = new MapUpdater();
		mu.inputWorldStateQueue = pr.outputWorldStateQueue = new LinkedBlockingQueue<WorldState>();
		mu.outputWorldStateQueue = pr.inputWorldStateQueue = new LinkedBlockingQueue<WorldState>();
		pr.inputWorldStateQueue.put(ws);
		
		final MapCanvas c = new MapCanvas();
		mu.updateListener = c;
		c.setBackground( Color.GRAY );
		//c.setPreferredSize( new Dimension(512,384) );
		c.setWorldState( ws );

		Apallit app = new Apallit( "SimpleSim", c );
		app.addService(pr);
		app.addService(br);
		app.addService(mu);
		app.runWindowed();
	}
}
