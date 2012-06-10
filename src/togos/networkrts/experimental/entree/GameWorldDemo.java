package togos.networkrts.experimental.entree;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import togos.networkrts.awt.Apallit;
import togos.networkrts.awt.TimestampedPaintable;
import togos.networkrts.experimental.s64.Blocks;
import togos.networkrts.experimental.s64.Block;
import togos.networkrts.experimental.s64.Circle;
import togos.networkrts.experimental.s64.GridNode64;
import togos.networkrts.experimental.s64.GridWorld64Viewer;
import togos.networkrts.experimental.s64.Shape;
import togos.networkrts.experimental.s64.UnionShape;
import togos.networkrts.tfunc.ConstantPositionFunction;
import togos.networkrts.tfunc.ConstantScalarFunction;
import togos.networkrts.tfunc.PositionFunction;
import togos.networkrts.tfunc.ScalarFunction;
import togos.networkrts.tfunc.Simple2DTransform;
import togos.networkrts.tfunc.Simple2DTransformFunction;
import togos.networkrts.util.TMath;

public class GameWorldDemo extends Apallit
{
	private static final long serialVersionUID = 5975810881860478072L;
	
	static class GameEntity extends AbstractPlaneEntity implements AWTDrawableEntity
	{
		public static final int LAYER_0 = 0x01000000;
		public static final int LAYER_1 = 0x02000000;
		public static final int LAYER_2 = 0x04000000;
		public static final int LAYER_3 = 0x08000000;
		public static final int LAYER_4 = 0x10000000;
		public static final int LAYER_5 = 0x20000000;
		public static final int LAYER_6 = 0x40000000;
		public static final int LAYER_7 = 0x80000000;
		public static final int[] LAYER_FLAGS = {
			LAYER_0, LAYER_1, LAYER_2, LAYER_3, LAYER_4, LAYER_5, LAYER_6, LAYER_7
		};
		public static final int LAYER_COUNT = LAYER_FLAGS.length;
		public static final GameEntity[] EMPTY_LIST = new GameEntity[0];
		
		final int outerArmor = 100;
		final int innerArmor = 100;
		final int health     = 100;
		final GameEntity[] subEntities;
		final ScalarFunction rotation;
		final double maxRadius;
		final AWTDrawable drawable;
		
		static int aggregateFlags( GameEntity[] entities ) {
			int flags = 0;
			for( GameEntity e : entities ) {
				flags |= e.getFlags();
			}
			return flags;
		}
		
		static double aggregateMaxRadius( double maxRad, GameEntity[] entities ) {
			for( GameEntity e : entities ) {
				double x = e.getX(), y = e.getY();
				double rad = Math.sqrt( x*x + y*y ) + e.maxRadius;
				if( rad > maxRad ) maxRad = rad;
			}
			return maxRad;
		}
		
		public GameEntity( Object planeId, long referenceTimestamp, PositionFunction pf, ScalarFunction rotation, int flags, AWTDrawable drawable, GameEntity[] subEntities ) {
			super( planeId, referenceTimestamp, pf, rotation, flags | aggregateFlags(subEntities) );
			this.rotation = rotation;
			this.subEntities = subEntities;
			this.drawable = drawable;
			this.maxRadius = aggregateMaxRadius( 0.1, subEntities );
		}
		
		@Override public double getMaxRadius() {  return maxRadius;  }
		
		@Override
		public void draw(Graphics2D g2d, float x, float y, float scale, float rotation, long timestamp, int renderLayer) {
			drawable.draw(g2d, x, y, scale, rotation, timestamp, renderLayer);
			double sin = Math.sin(rotation);
			double cos = Math.cos(rotation);
			double[] pbuf = new double[3];
			for( GameEntity e : subEntities ) {
				e.position.getPosition( timestamp, pbuf );
				e.draw(g2d, (float)(x + cos * pbuf[0] - sin * pbuf[1]), (float)(y + sin * pbuf[0] + cos * pbuf[1]), scale, rotation, timestamp, renderLayer);
			}
		}
	}
	
	static class GameState {
		final double width, height;
		/** Size of entity and terrain root nodes */
		final double rootNodeSize;
		final EntityQuadTreeNode entities;
		final GridNode64 terrain;
		
		public GameState( double width, double height, double rootNodeSize, EntityQuadTreeNode entities, GridNode64 terrain ) {
			this.width = width;
			this.height = height;
			this.rootNodeSize = rootNodeSize;
			this.entities = entities;
			this.terrain = terrain;
		}
		
		public GameState updateEntities( EntityPlaneUpdate up ) {
			return new GameState(
				width, height, rootNodeSize,
				entities.update( up, 0, 0, rootNodeSize ),
				terrain
			);
		}
		
		public GameState withEntity( GameEntity ent ) {
			return updateEntities( new EntityPlaneUpdate( new GameEntity[0], new GameEntity[] { ent } ));
		}
	}
	
	class GameWorldDrawer implements TimestampedPaintable {
		Simple2DTransformFunction tf = new Simple2DTransform( 128, 128, 128, 0, 8 );
		GameState gs = new GameState( 256, 256, 256, EntityQuadTreeNode.EMPTY, Blocks.GRASS.getHomogeneousNode() );
		
		public void setState( GameState gs ) {
			this.gs = gs;
		}
		public void setTransform( Simple2DTransformFunction tf ) {
			this.tf = tf;
		}
		
		@Override
		public void paint( final long timestamp, final int width, final int height, final Graphics2D g) {
			final Simple2DTransform xf = Simple2DTransform.from(tf, timestamp);
			
			if( gs == null ) return;
			
			Rectangle gClip = g.getClipBounds();
			ClipRectangle wClip = new ClipRectangle(
				xf.x + (gClip.getMinX() - width/2) / xf.scale,
				xf.y + (gClip.getMinY() - height/2) / xf.scale,
				gClip.getWidth() / xf.scale,
				gClip.getHeight() / xf.scale
			);
			
			// World screen coordinates
			final double wsx = -xf.x*xf.scale + width/2;
			final double wsy = -xf.y*xf.scale + height/2;
			
			GridWorld64Viewer.paintAt(
				gs.terrain, g, wsx, wsy,
				xf.scale * gs.rootNodeSize,
				timestamp
			);
			for( int i=0; i<GameEntity.LAYER_COUNT; ++i ) {
				final int layerNumber = i;
				gs.entities.eachEntity( wClip, GameEntity.LAYER_FLAGS[i], 0, 0, 0, gs.rootNodeSize, new Iterated() {
					/** Position buffer */
					double[] pbuf = new double[3];
					
					@Override
					public void item( Object o ) {
						AWTDrawableEntity e = (AWTDrawableEntity)o;
						e.getPosition( timestamp, pbuf );
						e.draw( (Graphics2D)g,
							(float)((pbuf[0] - xf.x)*xf.scale + width/2),
							(float)((pbuf[1] - xf.x)*xf.scale + height/2),
							(float)xf.scale, (float)e.getRotation(timestamp), timestamp, layerNumber
						);
					}
				} );
			}
		}
	}
	
	GameWorldDrawer drawer = new GameWorldDrawer();
	
	static class TreeDrawable implements AWTDrawable {
		public static final Node[] EMPTY_NODE_LIST = new Node[0];
		
		static class Node {
			public final float dx, dy;
			public final float size;
			public final Color color;
			public final Node[] subNodes;
			
			public Node( float dx, float dy, float size, Color color, Node[] subNodes ) {
				this.dx = dx; this.dy = dy;
				this.size = size;
				this.color = color;
				this.subNodes = subNodes;
			}
			
			public final void draw( Graphics g, float baseX, float baseY, float scale, long timestamp, long oscPeriod, long oscOffset ) {
				float cx = baseX + (dx + 0.2f*TMath.periodic( (int)timestamp + oscOffset, oscPeriod ))*scale;
				float cy = baseY + (dy + 0.2f*TMath.periodic( (int)timestamp + oscOffset, oscPeriod + oscOffset ))*scale;
				g.setColor( color );
				g.fillOval( (int)(cx - scale*size/2), (int)(cy - scale*size/2), (int)(size*scale), (int)(size*scale) );
				for( int i=0; i<subNodes.length; ++i ) {
					subNodes[i].draw( g, cx, cy, scale, timestamp, oscPeriod/2, oscOffset + i*oscPeriod/subNodes.length );
				}
			}
		}
		
		public final Node root;
		public TreeDrawable( Node root ) {
			this.root = root;
		}
		
		@Override
		public void draw( Graphics2D g2d, float x, float y, float scale,
				float rotation, long timestamp, int renderLayer ) {
			root.draw( g2d, x, y, scale, timestamp, 8000, 0 );
		}
		
		static final Random r = new Random();
		static final Color[] LEAF_COLORS = new Color[] {
			new Color( 0f, 0.45f, 0.1f ),
			new Color( 0f, 0.50f, 0.1f ),
			new Color( 0f, 0.55f, 0.1f )
		};
		public static Node generate( int depth, int nChildren ) {
			Node[] subNodes;
			if( depth == 0 ) {
				subNodes = EMPTY_NODE_LIST;
			} else {
				subNodes = new Node[nChildren];
				for( int i=0; i<subNodes.length; ++i ) {
					subNodes[i] = generate( depth-1, nChildren );
				}
			}
			return new Node( (r.nextFloat() - 0.5f) * 2, (r.nextFloat() - 0.5f) * 2, 2.0f, LEAF_COLORS[r.nextInt(3)], subNodes );
		}
	}
	
	Random r = new Random(1234);
	protected void addTrees( GridNode64 terrain, double x, double y, double size, List<double[]> atCoords ) {
		double subSize = size/8;
		if( size <= 8 ) {
			for( int sy=0; sy<8; ++sy ) {
				for( int sx=0; sx<8; ++sx ) {
					Block[] stack = terrain.blockStacks[sx + 8 * sy];
					boolean canPlaceHere = false;
					for( int z=0; z<stack.length; ++z ) {
						if( (stack[z].flags & Block.FLAG_WALKABLE) != 0 ) {
							canPlaceHere = true; break;
						}
					}
					if( canPlaceHere && r.nextInt(64) < 1 ) {
						atCoords.add( new double[]{ x + (sx+0.5)*subSize, y + (sx+0.5)*subSize } );
					}
				}
			}
		} else {
			for( int sy=0; sy<8; ++sy ) {
				for( int sx=0; sx<8; ++sx ) {
					addTrees( terrain.subNodes[sx + 8 * sy], x + sx*subSize, y + sy*subSize, subSize, atCoords );
				}
			}
		}
	}
	
	@Override
	public void init() {
		fillWith( drawer, 30 );
		GridNode64 terrain = Blocks.GRASS.getHomogeneousNode();
		for( int i=0; i<128; ++i ) {
			Shape[] shapes = new Shape[4];
			for( int j=0; j<4; ++j ) {
				Circle c = new Circle( r.nextDouble()*256, r.nextDouble()*256, r.nextDouble()*r.nextDouble()*16);
				shapes[j] = c;
			}
			terrain = terrain.fillArea( 256, 0, 0, new UnionShape(shapes), 1, Blocks.WATER_FILLER );
		}
		ArrayList<double[]> treeLocations = new ArrayList<double[]>();
		addTrees( terrain, 0, 0, 256, treeLocations );
		GameState gs = new GameState( 256, 256, 256,
			EntityQuadTreeNode.EMPTY,
			terrain
		);
		for( double[] treeLoc : treeLocations ) {
			gs = gs.withEntity( new GameEntity(
				"x", 0, new ConstantPositionFunction(treeLoc[0],treeLoc[1],0), new ConstantScalarFunction(0), PlaneEntity.FLAG_EXISTS | GameEntity.LAYER_4,
				new TreeDrawable( TreeDrawable.generate(2, 3)), GameEntity.EMPTY_LIST
			) );
		}
		drawer.setState( gs );
		super.init();
	}
	
	public static void main( String[] args ) {
		new GameWorldDemo().runWindowed();
	}
}
