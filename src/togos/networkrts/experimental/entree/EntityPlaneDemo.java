package togos.networkrts.experimental.entree;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.ScrollPane;
import java.util.Random;

import togos.networkrts.awt.Apallit;
import togos.networkrts.tfunc.ColorFunction;
import togos.networkrts.tfunc.ConstantColorFunction;
import togos.networkrts.tfunc.ConstantPositionFunction;
import togos.networkrts.tfunc.ConstantScalarFunction;
import togos.networkrts.tfunc.PulsatingColorFunction;
import togos.service.InterruptableSingleThreadedService;

public class EntityPlaneDemo extends Apallit
{
	private static final long serialVersionUID = 1;

	static class SimpleEntity extends AbstractPlaneEntity implements AWTDrawableEntity {
		public final double radius;
		public final ColorFunction color;
		
		public SimpleEntity( String id, String planeId, double x, double y, double radius, int flags, ColorFunction color ) {
			super( id, planeId, 0, new ConstantPositionFunction(x, y, 0), ConstantScalarFunction.ZERO, flags|PlaneEntity.FLAG_EXISTS );
			this.radius = radius;
			this.color = color;
		}
		
		public int getFlags() {  return flags;  }
		public double getMaxRadius() {  return radius;  }
		
		public void draw(Graphics2D g2d, float x, float y, float scale, float rotation, long timestamp, int layer) {
			g2d.setColor( color.getAwtColor(timestamp) );
			g2d.fillOval( (int)(x - radius), (int)(y - radius), (int)(radius*2), (int)(radius*2) );
		}
	}
	
	EntityPlaneCanvas canvas;
	
	public EntityPlaneDemo() {
		super("EntityPlaneDemo");
		
		canvas = new EntityPlaneCanvas();
		
		ColorFunction[] colors = new ColorFunction[] {
			new ConstantColorFunction(Color.RED),
			new ConstantColorFunction(Color.ORANGE),
			new ConstantColorFunction(Color.YELLOW),
			new ConstantColorFunction(Color.GREEN),
			new PulsatingColorFunction(
				0.5f, 0.5f, 0.0f, 0.0f,
				1.0f, 1.0f, 1.0f, 1.0f,
				4000, 1000
			),
			new ConstantColorFunction(Color.BLUE),
			new ConstantColorFunction(Color.PINK),
			new ConstantColorFunction(Color.WHITE)
		};
		
		EntityPlane<SimpleEntity> plane = new QuadTreeEntityPlane( 65536, 65536, EntityQuadTreeNode.EMPTY, 65536 );
		for( int i=0; i<65536*8; ++i ) {
			Random r = new Random();
			int layer = r.nextInt(8);
			
			plane = plane.update( new EntityPlaneUpdate(
				EntityPlaneUpdate.EMPTY_ENTITY_LIST,
				new SimpleEntity[] {
					new SimpleEntity(
						String.valueOf(i), "x",
						Math.random() * 32768, Math.random() * 32768,
						Math.random() * Math.random() * 128,
						layer << 1,
						colors[layer]
					),
				}
			) );
		}
		
		canvas.setState( plane, 16384d, 16384d, 2.0 );
		canvas.setBackground(Color.BLACK);
		canvas.setSize(32768, 32768);
		
		final ScrollPane sp = new ScrollPane();
		sp.setPreferredSize( new Dimension(512,384) );
		sp.add(canvas);
		
		fillWith(sp);

		addService( new InterruptableSingleThreadedService() {
			@Override
			protected void _run() throws InterruptedException {
				while( !Thread.interrupted() ) {
					Point p = sp.getScrollPosition();
					Dimension vps = sp.getViewportSize();
					canvas.repaint( p.x, p.y, vps.width, vps.height );
					Thread.sleep(30);
				}
			}
		} );
	}
	
	@Override
	public void init() {
		super.init();
	}
		
	public static void main( String[] args ) {
		new EntityPlaneDemo().runWindowed();
	}
}
