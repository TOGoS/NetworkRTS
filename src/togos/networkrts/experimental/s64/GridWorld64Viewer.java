package togos.networkrts.experimental.s64;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

public class GridWorld64Viewer extends Applet
{
	Block GRASS = new Block(null, Block.FLAG_WALKABLE, Color.GREEN);
	Block WATER = new Block(null, Block.FLAG_BOATABLE, Color.BLUE);
	
	private static final long serialVersionUID = 1L;
	
	public GridWorld64 world = GridWorld64.EMPTY;
	
	public void init() {
		/*
		setWorld(
			world.
			withBlock(0, 0, 0, WATER ).
			withBlock(3, 1, 1, GRASS ).
			withBlock(6, 9, 9, WATER ).
			withBlock( new Circle(0.5, 0.5, 0.5), 0.01, GRASS )
		);
		*/
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
		final Frame f = new Frame("GridWorldViewer");
		final GridWorld64Viewer gwv = new GridWorld64Viewer();
		gwv.init();
		gwv.setPreferredSize(new Dimension(640,480));
		f.add(gwv);
		f.pack();
		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				f.dispose();
			}
		});
		f.setVisible(true);
	}
}
