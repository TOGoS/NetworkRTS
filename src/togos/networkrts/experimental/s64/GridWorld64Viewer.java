package togos.networkrts.experimental.s64;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Random;

import togos.networkrts.awt.Apallit;
import togos.networkrts.awt.TimestampedPaintable;
import togos.networkrts.experimental.shape.TCircle;
import togos.networkrts.experimental.shape.RectIntersector;
import togos.networkrts.experimental.shape.TUnion;

public class GridWorld64Viewer extends Apallit implements TimestampedPaintable
{
	private static final long serialVersionUID = 1L;
	
	public GridWorld64 world = GridWorld64.EMPTY;
	
	public GridWorld64Viewer() {
		super("GridWorld64Viewer");
		setPreferredSize( new Dimension(640,480) );
	}
	
	public void init() {
		super.init();
		
		Random r = new Random();
		RectIntersector[] shapes = new RectIntersector[20];
		for( int j=0; j<20; ++j ) {
			for( int i=0; i<20; ++i ) {
				TCircle c = new TCircle(0.2 + r.nextDouble() * 0.5, 0.2 + r.nextDouble() * 0.6, r.nextDouble()*r.nextDouble()*0.1);
				shapes[i] = c;
			}
			setWorld( world.fillArea( new TUnion(shapes), 0.001, r.nextBoolean() ? Blocks.WATER_FILLER : Blocks.GRASS.getFiller() ) );
		}
		
		fillWith( this, 50 );
	}
	
	public static void paintAt( GridNode64 n, Graphics g, double x, double y, double size, long timestamp ) {
		if( size > 256 ) {
			Rectangle r = g.getClipBounds();
			if( x + size <= r.x ) return;
			if( y + size <= r.y ) return;
			if( x >= r.x + r.width ) return;
			if( y >= r.y + r.height ) return;
		}
		
		double subSize = size / 8;
		if( (size >= 64 && !n.isHomogeneous()) || size > 1024 ) {
			for( int sy=0, i=0; sy<8; ++sy ) {
				for( int sx=0; sx<8; ++sx, ++i ) {
					paintAt( n.subNodes[i], g, x + sx * subSize, y + sy * subSize, subSize, timestamp );
				}
			}
		} else if( n.isHomogeneous() || size <= 4 ) {
			int drawSize = (int)Math.ceil( size );
			Block[] stack = n.blockStacks[0];
			for( int j=0; j<stack.length; ++j ) {
				g.setColor( stack[j].getColorFunction().getAwtColor(timestamp) );
				g.fillRect( (int)x, (int)y, drawSize, drawSize );
			}
		} else {
			int drawSize = (int)Math.ceil( subSize );
			for( int sy=0, i=0; sy<8; ++sy ) {
				for( int sx=0; sx<8; ++sx, ++i ) {
					Block[] stack = n.blockStacks[i];
					for( int j=0; j<stack.length; ++j ) {
						g.setColor( stack[j].getColorFunction().getAwtColor(timestamp) );
						g.fillRect( (int)(x + sx * subSize), (int)(y + sy * subSize), drawSize, drawSize);
					}
				}
			}
		}
	}
	
	public void paintAt( Graphics g, int x, int y, int size, long timestamp ) {
		paintAt( world.topNode, g, x, y, size, timestamp );
	}
	
	public void paint(long timestamp, int width, int height, java.awt.Graphics2D g2d) {
		paintAt( g2d, 0, 0, 1024, timestamp );
	};
	
	public void setWorld( GridWorld64 world ) {
		this.world = world;
		repaint();
	}
	
	public static void main( String[] args ) {
		new GridWorld64Viewer().runWindowed();
	}
}
