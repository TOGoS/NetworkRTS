package togos.networkrts.experimental.entree;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.ScrollPane;
import java.util.Random;

import togos.networkrts.awt.Apallit;

public class EntityPlaneDemo extends Apallit
{
	private static final long serialVersionUID = 1;

	static class SimpleEntity implements AWTDrawableEntity, PlaneEntity {
		public final String id;
		public final String planeId;
		public final double x, y, radius;
		public final int flags;
		public final Color color;
		
		public SimpleEntity( String id, String planeId, double x, double y, double radius, int flags, Color color ) {
			this.id = id;
			this.planeId = planeId;
			this.x = x; this.y = y;
			this.radius = radius;
			this.flags = flags | PlaneEntity.FLAG_EXISTS;
			this.color = color;
		}
		
		public int getFlags() {  return flags;  }
		public Object getId() {  return id;  }
		public double getMaxRadius() {  return radius;  }
		public Object getPlaneId() {  return planeId;  }
		public double getX() {  return x;  }
		public double getY() {  return y;  }
		public Color getColor() {  return color;  }
		
		public void draw(Graphics2D g2d, double x, double y, double scale) {
			g2d.setColor( color );
			g2d.fillOval( (int)(x - radius), (int)(y - radius), (int)(radius*2), (int)(radius*2) );
		}
	}
	
	public EntityPlaneDemo() {
		super("EntityPlaneDemo");
		
		EntityPlaneCanvas c = new EntityPlaneCanvas();
		
		Color[] colors = new Color[] {
			Color.RED,
			Color.ORANGE,
			Color.YELLOW,
			Color.GREEN,
			Color.CYAN,
			Color.BLUE,
			Color.PINK,
			Color.WHITE
		};
		
		EntityPlane<SimpleEntity> plane = new QuadTreeEntityPlane( 65536, 65536, EntityQuadTreeNode.EMPTY, 65536 );
		for( int i=0; i<65536; ++i ) {
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
		
		c.setState( plane, 16384d, 16384d, 2.0 );
		c.setBackground(Color.BLACK);
		c.setSize(32768, 32768);
		
		ScrollPane sp = new ScrollPane();
		sp.setPreferredSize( new Dimension(512,384) );
		sp.add(c);
		
		fillWith(sp);
	}
		
	public static void main( String[] args ) {
		new EntityPlaneDemo().runWindowed();
	}
}
