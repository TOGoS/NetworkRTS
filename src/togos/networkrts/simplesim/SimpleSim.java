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
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import togos.service.InterruptableSingleThreadedService;
import togos.service.ServiceManager;

public class SimpleSim
{
	static class Event {
		public static final int TICK = 0;
		public static final int ATTEMPT_MOVE = 1;
		
		final int verb;
		final int arg;
		
		public Event( int verb, int arg ) {
			this.verb = verb;
			this.arg = arg;
		}
	}
	
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
	
	static class Block {
		public static final Block EMPTY = new Block( "empty", Behavior.NONE, Color.BLACK, Collections.EMPTY_LIST );
		public static final Block STONE = new Block( "stone", Behavior.NONE, Color.DARK_GRAY, Collections.EMPTY_LIST );
		
		public final String id;
		public final Behavior behavior;
		public final Color color;
		public final List commands;
		
		public Block( String id, Behavior b, Color c, List commands ) {
			this.id = id;
			this.behavior = b;
			this.color = c;
			this.commands = commands;
		}
	}
	
	static class Map {
		public final int width, height; // Must be powers of 2!!!
		public final Block[] blocks;
		
		public Map( int width, int height, Block[] blocks ) {
			if( width * height != blocks.length ) {
				throw new RuntimeException("blocks array should have length = width ("+width+") * height ("+height+"), but is "+blocks.length);
			}
			this.width = width;
			this.height = height;
			this.blocks = blocks;
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
	
	static class PhysicsRunner extends InterruptableLoopingService {
		public BlockingQueue inputWorldStateQueue;
		public BlockingQueue outputWorldStateQueue;
		
		long lastRunTime = -1;
		long interval = 100;
		
		static final Event TICK = new Event( Event.TICK, 0 );
		
		protected void runOneIteration() throws InterruptedException {
			long curTime = System.currentTimeMillis();
			
			if( lastRunTime != -1 && lastRunTime + interval > curTime ) {
				Thread.sleep( lastRunTime + interval - curTime );
			}
			
			lastRunTime = System.currentTimeMillis();
				
			Map s = (Map)inputWorldStateQueue.take();
			
			Block[] newBlocks = new Block[s.width*s.height];
			for( int y=s.height-1; y>=0; --y ) {
				for( int x=s.width-1; x>=0; --x ) {
					newBlocks[x+y*s.width] = s.blocks[x+y*s.width]; 
				}
			}
			
			for( int y=s.height-1; y>=0; --y ) {
				for( int x=s.width-1; x>=0; --x ) {
					Block b = s.blocks[x+y*s.width];
					CommandSet cs;
					if( b.behavior != Behavior.NONE && (cs = b.behavior.onEvent(TICK)) != CommandSet.EMPTY ) {
						int movementDir = -1;
						for( Iterator ci=cs.commands.iterator(); ci.hasNext(); ) {
							Event e = (Event)ci.next();
							if( e.verb == Event.ATTEMPT_MOVE ) {
								movementDir = e.arg;
							}
						}
						
						Behavior newBehavior = cs.newBehavior == null ? b.behavior : cs.newBehavior;
						Block newBlock = new Block( b.id, newBehavior, b.color, Collections.EMPTY_LIST );
						newBlocks[x+y*s.width] = newBlock; // but see movement handling, below
						
						int dx, dy;
						switch( movementDir ) {
						case( 0 ): dx =  1; dy =  0; break;
						case( 2 ): dx =  0; dy =  1; break;
						case( 4 ): dx = -1; dy =  0; break;
						case( 6 ): dx =  0; dy = -1; break;
						default:   dx =  0; dy =  0; break;
						}
						
						if( dx != 0 || dy != 0 ) {
							int nx = (x+dx) & (s.width-1);
							int ny = (y+dy) & (s.height-1);
							if( newBlocks[nx+ny*s.width] == Block.EMPTY ) {
								newBlocks[nx+ny*s.width] = newBlock;
								newBlocks[x+y*s.width] = Block.EMPTY;
							}
						}
					}
				}
			}
			
			outputWorldStateQueue.put( new Map(s.width, s.height, newBlocks) );
		}
	}
	
	interface WorldUpdatable {
		public void setWorldState( Map s );
	}
	
	static class MapCanvas extends Canvas implements WorldUpdatable {
		Map state = new Map(0, 0, new Block[0]);
		
		public synchronized void setWorldState( Map s ) {
			if( s == state ) return;
			state = s;
			repaint();
		}
		
		public void paint( Graphics g ) {
			final Map s = state;
			
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
				Map m = (Map)inputWorldStateQueue.take();
				outputWorldStateQueue.put(m);
				updateListener.setWorldState(m);
			}
		}
	}
	
	public static void main( String[] args ) throws InterruptedException {
		final ServiceManager sman = new ServiceManager();
		
		final Behavior wanderator = new Behavior() {
			public CommandSet onEvent(Event e) {
				List cmds = new ArrayList();
				cmds.add( new Event(Event.ATTEMPT_MOVE, new Random().nextInt(8) ));
				return new CommandSet( cmds, this );
			}
		};
		final Block wanderer = new Block( "wanderer", wanderator, Color.YELLOW, Collections.EMPTY_LIST );
		
		Map map = new Map(4,4,new Block[]{
			Block.STONE, Block.STONE, Block.EMPTY, Block.STONE,
			Block.STONE, Block.EMPTY, Block.EMPTY, Block.STONE,
			Block.EMPTY, Block.EMPTY, Block.STONE, Block.EMPTY,
			Block.STONE, Block.STONE,    wanderer, Block.EMPTY,
		});
		
		PhysicsRunner pr = new PhysicsRunner();
		
		MapUpdater mu = new MapUpdater();
		mu.inputWorldStateQueue = pr.outputWorldStateQueue = new LinkedBlockingQueue();
		mu.outputWorldStateQueue = pr.inputWorldStateQueue = new LinkedBlockingQueue();
		pr.inputWorldStateQueue.put(map);
		
		final Frame f = new Frame("SimpleSim");
		final MapCanvas c = new MapCanvas();
		mu.updateListener = c;
		c.setPreferredSize( new Dimension(512,384) );
		c.setWorldState( map );
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
