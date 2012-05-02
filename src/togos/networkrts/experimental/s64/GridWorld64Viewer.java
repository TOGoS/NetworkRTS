package togos.networkrts.experimental.s64;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Random;

import togos.networkrts.awt.Apallit;

public class GridWorld64Viewer extends Apallit
{
	Block GRASS = new Block(null, Block.FLAG_WALKABLE, Color.GREEN);
	Block WATER = new Block(null, Block.FLAG_BOATABLE, Color.BLUE);
	
	private static final long serialVersionUID = 1L;
	
	public GridWorld64 world = GridWorld64.EMPTY;
	
	public GridWorld64Viewer() {
		super("GridWorld64Viewer");
		setPreferredSize( new Dimension(640,480) );
	}
	
	public void init() {
		super.init();
		Random r = new Random();
		Shape[] shapes = new Shape[1000];
		for( int i=0; i<1000; ++i ) {
			shapes[i] = new Circle(0.2 + r.nextDouble() * 0.5, 0.2 + r.nextDouble() * 0.6, r.nextDouble()*0.1);
		}
		setWorld( world.withBlock( new UnionShape(shapes), 0.001, r.nextBoolean() ? WATER : GRASS ) );
	}
	
	public void paintAt( GridNode64 n, Graphics g, int x, int y, int zoomPower ) {
		int subSizePower = zoomPower - 3;
		for( int sy=0, i=0; sy<8; ++sy ) {
			for( int sx=0; sx<8; ++sx, ++i ) {
				Block[] stack = n.blockStacks[i];
				for( int j=0; j<stack.length; ++j ) {
					g.setColor( stack[j].getColor() );
					g.fillRect( x + (sx << subSizePower), y + (sy << subSizePower), 1 << subSizePower, 1 << subSizePower);
				}
			}
		}
		if( zoomPower > 4 ) {
			for( int sy=0, i=0; sy<8; ++sy ) {
				for( int sx=0; sx<8; ++sx, ++i ) {
					if( n.subNodes[i] != n ) {
						paintAt( n.subNodes[i], g, x + (sx << subSizePower), y + (sy << subSizePower), subSizePower );
					}
				}
			}
		}
	}
	
	public void paintAt( Graphics g, int x, int y, int zoomPower ) {
		paintAt( world.topNode, g, x, y, zoomPower + world.topNodeSizePower ); 
	}
	
	@Override
	public void paint( Graphics g ) {
		g.setColor( Color.BLACK );
		g.fillRect( 0, 0, getWidth(), getHeight() );
		
		paintAt( g, 0, 0, 10 );
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
