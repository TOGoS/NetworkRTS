package togos.networkrts.experimental.s64;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Random;

import togos.networkrts.awt.Apallit;
import togos.networkrts.tfunc.ConstantColorFunction;

public class GridWorld64Viewer extends Apallit
{
	Block GRASS = new Block(null, Block.FLAG_WALKABLE, new ConstantColorFunction(Color.GREEN));
	Block WATER = new Block(null, Block.FLAG_BOATABLE, new ConstantColorFunction(Color.BLUE));
	
	private static final long serialVersionUID = 1L;
	
	public GridWorld64 world = GridWorld64.EMPTY;
	
	public GridWorld64Viewer() {
		super("GridWorld64Viewer");
		setPreferredSize( new Dimension(640,480) );
	}
	
	public void init() {
		super.init();
		
		//Circle c = new Circle( 0.5, 0.5, 0.25 );
		//setWorld( world.withBlock(c, 1 / 1024.0, WATER ));
		
		Random r = new Random();
		Shape[] shapes = new Shape[20];
		for( int j=0; j<20; ++j ) {
			for( int i=0; i<20; ++i ) {
				Circle c = new Circle(0.2 + r.nextDouble() * 0.5, 0.2 + r.nextDouble() * 0.6, r.nextDouble()*0.1);
				shapes[i] = c;
				//setWorld( world.withBlock(c, 0.001, r.nextBoolean() ? WATER : GRASS));
			}
			setWorld( world.fillArea( new UnionShape(shapes), 0.001, r.nextBoolean() ? WATER.getRecursiveNode() : GRASS.getRecursiveNode() ) );
		}
	}
	
	public void paintAt( GridNode64 n, Graphics g, int x, int y, int size, long timestamp ) {
		int subSize = size >> 3;
		if( (size >= 64 && !n.isHomogeneous()) || size > 1024 ) {
			for( int sy=0, i=0; sy<8; ++sy ) {
				for( int sx=0; sx<8; ++sx, ++i ) {
					paintAt( n.subNodes[i], g, x + sx * subSize, y + sy * subSize, subSize, timestamp );
				}
			}
		} else if( n.isHomogeneous() || size <= 4 ) {
			Block[] stack = n.blockStacks[0];
			for( int j=0; j<stack.length; ++j ) {
				g.setColor( stack[j].getColorFunction().getAwtColor(timestamp) );
				g.fillRect( x, y, size, size );
			}
		} else {
			int drawSize = (int)Math.ceil( size / 8.0 );
			for( int sy=0, i=0; sy<8; ++sy ) {
				for( int sx=0; sx<8; ++sx, ++i ) {
					Block[] stack = n.blockStacks[i];
					for( int j=0; j<stack.length; ++j ) {
						g.setColor( stack[j].getColorFunction().getAwtColor(timestamp) );
						g.fillRect( x + ((sx * size) >> 3), y + ((sy * size) >> 3), drawSize, drawSize);
					}
				}
			}
		}
	}
	
	public void paintAt( Graphics g, int x, int y, int size ) {
		paintAt( world.topNode, g, x, y, size, System.currentTimeMillis() );
	}
	
	@Override
	public void paint( Graphics g ) {
		g.setColor( Color.BLACK );
		g.fillRect( 0, 0, getWidth(), getHeight() );
		
		paintAt( g, 0, 0, 1024 );
	}
	
	@Override
	public void update( Graphics g ) {
		paint( g );
	}
	
	public void setWorld( GridWorld64 world ) {
		this.world = world;
		repaint();
	}
	
	public static void main( String[] args ) {
		new GridWorld64Viewer().runWindowed();
	}
}
