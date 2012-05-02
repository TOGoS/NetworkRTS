package togos.networkrts.tfunc;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import togos.networkrts.awt.Apallit;
import togos.networkrts.awt.TimestampedPaintable;

public class TFuncDemo extends Apallit
{
	private static final long serialVersionUID = 1L;
	
	abstract class ARGBolorFunction implements ColorFunction {
		public Color getAwtColor(long ts) {
			return new Color(getColor(ts), true);
		}
	}
	
	class ConstantColorFunction implements ColorFunction {
		protected final Color color;
		
		public ConstantColorFunction( Color c ) {
			this.color = c;
		}
		public Color getAwtColor(long ts) {
			return color;
		}
		public int getColor(long ts) {
			return color.getRGB();
		}
	}
	
	class ConstantPositionFunction implements PositionFunction {
		double x,y,z;
		
		public ConstantPositionFunction( double x, double y, double z ) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public void getPosition(long timestamp, double[] dest) {
			dest[0] = x;
			dest[1] = y;
			dest[2] = z;
		}
	}
	
	class FloatyThing {
		public final ColorFunction color;
		public final PositionFunction position;
		public final double radius;
		
		public FloatyThing( ColorFunction color, PositionFunction position, double radius ) {
			this.color = color;
			this.position = position;
			this.radius = radius;
		}
	}
	
	class FloatyThingPaintable implements TimestampedPaintable {
		public List<FloatyThing> floatyThings;
		
		public FloatyThingPaintable( List<FloatyThing> things ) {
			this.floatyThings = things;
		}
		
		public void paint(long timestamp, int width, int height, Graphics2D g2d) {
			Rectangle cb = g2d.getClipBounds();
			g2d.clearRect( cb.x, cb.y, cb.width, cb.height );
			
			double[] pos = new double[3];
			for( Iterator<FloatyThing> i=floatyThings.iterator(); i.hasNext(); ) {
				FloatyThing t = i.next();
				t.position.getPosition(timestamp, pos);
				g2d.setColor( t.color.getAwtColor(timestamp) );
				g2d.fillOval(
					(int)(width  / 2 + pos[0] - t.radius),
					(int)(height / 2 + pos[1] - t.radius),
					(int)(t.radius * 2),
					(int)(t.radius * 2)
				);
			}
		}
	}
	
	protected static final double loop( long ts, long interval ) {
		long v = ts % interval;
		return Math.sin( v * Math.PI * 2 / interval );
	}
	
	protected static final int clampComponent( int c ) {
		return c < 0 ? 0 : c > 255 ? 255 : c;
	}
	
	protected static final int color( int a, int r, int g, int b ) {
		return
			((clampComponent(a) & 0xFF) << 24) |
			((clampComponent(r) & 0xFF) << 16) |
			((clampComponent(g) & 0xFF) <<  8) |
			((clampComponent(b) & 0xFF) <<  0);
	}
	
	protected static final int color( double a, double r, double g, double b ) {
		return color( (int)(a*255), (int)(r*255), (int)(g*255), (int)(b*255) );
	}
	
	public void init() {
		setTitle("SGame");
		ArrayList<FloatyThing> things = new ArrayList<FloatyThing>();
		things.add( new FloatyThing( new ARGBolorFunction() {
			public int getColor(long ts) {
				return color( 1.0, loop(ts,2000), loop(ts+500,2000), loop(ts+1000,2000) );
			}
		}, new PositionFunction() {
			public void getPosition(long timestamp, double[] dest) {
				dest[0] = 96 * loop( timestamp, 700 );
				dest[1] = 96 * loop( timestamp, 1100 );
				dest[2] = 96 * loop( timestamp, 1300 );
			}
		}, 20) );
		fillWith( new FloatyThingPaintable( things ), 640, 480, 10 );
		
		super.init();
	}
}
